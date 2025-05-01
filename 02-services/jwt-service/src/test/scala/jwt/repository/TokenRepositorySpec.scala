package jwt.repository

import zio.*
import zio.test.*
import user.models.UserId
import jwt.models.RefreshToken
import java.time.Instant
import java.util.UUID
import io.getquill.*
import io.getquill.jdbczio.Quill
import javax.sql.DataSource
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import java.sql.Timestamp
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{ Level, Logger }

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

  def createTestRefreshToken(
    userId: String = "test-user",
    expireInSeconds: Long = 3600,
  ): RefreshToken =
    RefreshToken(
      token = s"token-${UUID.randomUUID()}",
      expiresAt = Instant.now().plusSeconds(expireInSeconds),
      userId = UserId(userId),
    )

  def spec =
    suite("TokenRepository with H2 Database")(
      test("saveRefreshToken should save token to database") {
        for
          repo <- ZIO.service[TokenRepository]
          token = createTestRefreshToken()
          _ <- repo.saveRefreshToken(token)
          retrieved <- repo.findByRefreshToken(token.token)
        yield assertTrue(
          retrieved.isDefined,
          retrieved.exists(_.token == token.token), // Сравниваем по значению токена
          retrieved.exists(_.userId.value == token.userId.value), // Сравниваем по ID пользователя
          // Проверяем, что время истечения близко (в пределах секунды)
          retrieved.exists(_.expiresAt.getEpochSecond == token.expiresAt.getEpochSecond),
        )
      },
      test("findByRefreshToken should return None for non-existent token") {
        for
          repo <- ZIO.service[TokenRepository]
          nonExistentToken = "non-existent-token"
          retrieved <- repo.findByRefreshToken(nonExistentToken)
        yield assertTrue(retrieved.isEmpty)
      },
      test("findByRefreshToken should return None for expired token") {
        for
          repo <- ZIO.service[TokenRepository]
          expiredToken = createTestRefreshToken(expireInSeconds = 0)
          _ <- repo.saveRefreshToken(expiredToken)
          _ <- TestClock.adjust(1.second)
          retrieved <- repo.findByRefreshToken(expiredToken.token)
        yield assertTrue(retrieved.isEmpty)
      },
      test("deleteByRefreshToken should remove token") {
        for
          repo <- ZIO.service[TokenRepository]
          token = createTestRefreshToken()
          _ <- repo.saveRefreshToken(token)
          beforeDelete <- repo.findByRefreshToken(token.token)
          _ <- repo.deleteByRefreshToken(token.token)
          afterDelete <- repo.findByRefreshToken(token.token)
        yield assertTrue(
          beforeDelete.isDefined,
          afterDelete.isEmpty,
        )
      },
      test("deleteAllByUserId should remove all user tokens") {
        for
          repo <- ZIO.service[TokenRepository]
          userId = "multi-token-user"
          userIdObj = UserId(userId)
          token1 = createTestRefreshToken(userId)
          token2 = createTestRefreshToken(userId)
          otherUserToken = createTestRefreshToken("other-user")
          _ <- repo.saveRefreshToken(token1)
          _ <- repo.saveRefreshToken(token2)
          _ <- repo.saveRefreshToken(otherUserToken)
          beforeDelete1 <- repo.findByRefreshToken(token1.token)
          beforeDelete2 <- repo.findByRefreshToken(token2.token)
          beforeDeleteOther <- repo.findByRefreshToken(otherUserToken.token)
          _ <- repo.deleteAllByUserId(userIdObj)
          afterDelete1 <- repo.findByRefreshToken(token1.token)
          afterDelete2 <- repo.findByRefreshToken(token2.token)
          afterDeleteOther <- repo.findByRefreshToken(otherUserToken.token)
        yield assertTrue(
          beforeDelete1.isDefined,
          beforeDelete2.isDefined,
          beforeDeleteOther.isDefined,
          afterDelete1.isEmpty,
          afterDelete2.isEmpty,
          afterDeleteOther.isDefined,
        )
      },
      test("cleanExpiredTokens should remove expired tokens") {
        for
          repo <- ZIO.service[TokenRepository]
          quillContext <- ZIO.service[Quill.Postgres[SnakeCase]]
          expiredToken = createTestRefreshToken(expireInSeconds = -1)
          validToken = createTestRefreshToken(expireInSeconds = 3600)
          _ <- repo.saveRefreshToken(expiredToken)
          _ <- repo.saveRefreshToken(validToken)
          findExpiredBeforeClean <- repo.findByRefreshToken(expiredToken.token)
          findValidBeforeClean <- repo.findByRefreshToken(validToken.token)
          _ <- repo.cleanExpiredTokens()
          validAfterClean <- repo.findByRefreshToken(validToken.token)
        yield assertTrue(
          findExpiredBeforeClean.isEmpty,
          findValidBeforeClean.isDefined,
          validAfterClean.isDefined,
        )
      },
    ).provide(testEnvLayer.mapError(TestFailure.die))
