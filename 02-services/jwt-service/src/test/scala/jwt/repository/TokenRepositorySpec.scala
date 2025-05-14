package jwt.repository

import zio.*
import zio.test.*
import user.models.UserId
import jwt.models.RefreshToken
import jwt.models.JwtRefreshToken
import java.time.Instant
import java.util.UUID
import io.getquill.*
import io.getquill.jdbczio.Quill
import javax.sql.DataSource
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{ Level, Logger }
import jwt.entity.RefreshTokenEntity
object TokenRepositorySpec extends ZIOSpecDefault:
  locally:
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    rootLogger.setLevel(Level.WARN)

    val hikariLogger = LoggerFactory.getLogger("com.zaxxer.hikari").asInstanceOf[Logger]
    hikariLogger.setLevel(Level.ERROR)

    val quillLogger = LoggerFactory.getLogger("io.getquill").asInstanceOf[Logger]
    quillLogger.setLevel(Level.ERROR)

  implicit val instantEncoder: MappedEncoding[Instant, Timestamp] =
    MappedEncoding(Timestamp.from)
  implicit val instantDecoder: MappedEncoding[Timestamp, Instant] =
    MappedEncoding(_.toInstant)

  // --- H2 Database Setup ---
  val h2DataSourceLayer: ZLayer[Any, Throwable, DataSource] =
    ZLayer.scoped:
      ZIO.acquireRelease {
        ZIO.attempt:
          val config = new HikariConfig()
          // Уникальное имя БД для каждого запуска тестов
          val dbName = s"test_db_${UUID.randomUUID()}"
          config.setJdbcUrl(s"jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
          config.setDriverClassName("org.h2.Driver")
          config.setUsername("sa")
          config.setPassword("")
          val ds = new HikariDataSource(config)

          val conn = ds.getConnection()
          conn.createStatement().execute("DROP TABLE IF EXISTS refresh_tokens")
          conn
            .createStatement()
            .execute:
              """CREATE TABLE refresh_tokens (
              id VARCHAR(36) PRIMARY KEY,
              user_id VARCHAR(255) NOT NULL,
              refresh_token VARCHAR(1024) NOT NULL,
              expires_at TIMESTAMP NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
            )"""
          conn.close()
          ds
      }(ds => ZIO.attemptBlocking(ds.close()).orDie)

  val quillLayer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  val commonDependenciesLayer: ZLayer[Any, Throwable, Quill.Postgres[SnakeCase]] =
    h2DataSourceLayer >>> quillLayer

  val tokenRepoLayer: ZLayer[Quill.Postgres[SnakeCase], Throwable, TokenRepository] =
    TokenRepositoryImpl.layer

  val testEnvLayer: ZLayer[Any, Throwable, TokenRepository & Quill.Postgres[SnakeCase]] =
    commonDependenciesLayer >>> (tokenRepoLayer ++ ZLayer
      .environment[Quill.Postgres[SnakeCase]])

  // Вспомогательные функции для создания кастомных типов
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException(s"Invalid UserId in test setup: $id"))

  private def unsafeJwtRefreshToken(token: String): JwtRefreshToken =
    JwtRefreshToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtRefreshToken in test setup: $token")
    )

  def createTestRefreshTokenEntity(
    userId: String = "test-user",
    expireInSeconds: Long = 3600,
  ): RefreshTokenEntity =
    RefreshTokenEntity(
      id = UUID.randomUUID().toString(),
      userId = unsafeUserId(userId).value,
      refreshToken = unsafeJwtRefreshToken(s"token-${UUID.randomUUID()}").value,
      expiresAt = Instant.now().plusSeconds(expireInSeconds),
      createdAt = Instant.now(),
    )

  def spec =
    suite("TokenRepository with H2 Database")(
      test("saveRefreshToken should save token to database") {
        for
          repo <- ZIO.service[TokenRepository]
          token = createTestRefreshTokenEntity()
          _ <- repo.saveRefreshToken(token)
          retrieved <- repo.findByRefreshToken(token.refreshToken)
        yield assertTrue(
          retrieved.token.value == token.refreshToken,
          retrieved.userId.value == token.userId,
          retrieved.expiresAt.toEpochMilli == token.expiresAt.toEpochMilli,
        )
      },
      test("findByRefreshToken should return None for non-existent token") {
        for
          repo <- ZIO.service[TokenRepository]
          nonExistentJwtToken = unsafeJwtRefreshToken("non-existent-token")
          retrieved <- repo.findByRefreshToken(nonExistentJwtToken.value).exit
        yield assertTrue(retrieved.isFailure)
      },
      test("findByRefreshToken should return None for expired token") {
        for
          repo <- ZIO.service[TokenRepository]
          expiredToken = createTestRefreshTokenEntity(expireInSeconds = 0)
          _ <- repo.saveRefreshToken(expiredToken)
          _ <- TestClock.adjust(1.second)
          retrieved <- repo.findByRefreshToken(expiredToken.refreshToken).exit
        yield assertTrue(retrieved.isFailure)
      },
      test("deleteByRefreshToken should remove token") {
        for
          repo <- ZIO.service[TokenRepository]
          token = createTestRefreshTokenEntity()
          _ <- repo.saveRefreshToken(token)
          beforeDelete <- repo.findByRefreshToken(token.refreshToken).exit
          _ <- repo.deleteByRefreshToken(token.refreshToken)
          afterDelete <- repo.findByRefreshToken(token.refreshToken).exit
        yield assertTrue(beforeDelete.isSuccess && afterDelete.isFailure)
      },
      test("deleteAllByUserId should remove all user tokens") {
        for
          repo <- ZIO.service[TokenRepository]
          userIdString = "multi-token-user"
          token1 = createTestRefreshTokenEntity(userIdString)
          token2 = createTestRefreshTokenEntity(userIdString)
          otherUserToken = createTestRefreshTokenEntity("other-user")
          _ <- repo.saveRefreshToken(token1)
          _ <- repo.saveRefreshToken(token2)
          _ <- repo.saveRefreshToken(otherUserToken)
          beforeDelete1 <- repo.findByRefreshToken(token1.refreshToken).exit
          beforeDelete2 <- repo.findByRefreshToken(token2.refreshToken).exit
          beforeDeleteOther <- repo.findByRefreshToken(otherUserToken.refreshToken).exit
          _ <- repo.deleteAllByUserId(userIdString)
          afterDelete1 <- repo.findByRefreshToken(token1.refreshToken).exit
          afterDelete2 <- repo.findByRefreshToken(token2.refreshToken).exit
          afterDeleteOther <- repo.findByRefreshToken(otherUserToken.refreshToken).exit
        yield assertTrue(
          beforeDelete1.isSuccess,
          afterDelete1.isFailure,
          beforeDelete2.isSuccess,
          afterDelete2.isFailure,
          beforeDeleteOther.isSuccess,
          afterDeleteOther.isSuccess,
        )
      },
      test("cleanExpiredTokens should remove expired tokens") {
        for
          repo <- ZIO.service[TokenRepository]
          quillContext <- ZIO.service[Quill.Postgres[SnakeCase]]
          expiredToken = createTestRefreshTokenEntity(expireInSeconds = -1)
          validToken = createTestRefreshTokenEntity(expireInSeconds = 3600)
          _ <- repo.saveRefreshToken(expiredToken)
          _ <- repo.saveRefreshToken(validToken)
          findExpiredBeforeClean <- repo.findByRefreshToken(expiredToken.refreshToken).exit
          findValidBeforeClean <- repo.findByRefreshToken(validToken.refreshToken).exit
          _ <- repo.cleanExpiredTokens()
          validAfterClean <- repo.findByRefreshToken(validToken.refreshToken).exit
        yield assertTrue(
          findExpiredBeforeClean.isFailure,
          findValidBeforeClean.isSuccess,
          validAfterClean.isSuccess,
        )
      },
    ).provide(testEnvLayer.mapError(TestFailure.die))
