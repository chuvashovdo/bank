package user.repository

import zio.*
import zio.test.*
import user.entity.UserEntity
import user.models.User
import user.models.UserId
import user.mapper.UserEntityMapper
import java.time.Instant
import com.zaxxer.hikari.*
import javax.sql.DataSource
import io.getquill.*
import io.getquill.jdbczio.Quill
import java.util.UUID
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{ Level, Logger }
import user.models.{ Email, FirstName, LastName }

object UserRepositorySpec extends ZIOSpecDefault:
  // Хелперы для создания кастомных типов в тестах/моках
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException("Invalid UserId in test setup"))
  private def unsafeEmail(email: String): Email =
    Email(email).getOrElse(throw new RuntimeException("Invalid Email in test setup"))
  private def unsafeFirstName(name: String): FirstName =
    FirstName(name).getOrElse(throw new RuntimeException("Invalid FirstName in test setup"))
  private def unsafeLastName(name: String): LastName =
    LastName(name).getOrElse(throw new RuntimeException("Invalid LastName in test setup"))

  // Подавляем логи от HikariCP и Quill
  locally:
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    rootLogger.setLevel(Level.WARN)

    val hikariLogger = LoggerFactory.getLogger("com.zaxxer.hikari").asInstanceOf[Logger]
    hikariLogger.setLevel(Level.ERROR)

    val quillLogger = LoggerFactory.getLogger("io.getquill").asInstanceOf[Logger]
    quillLogger.setLevel(Level.ERROR)

    val h2Logger = LoggerFactory.getLogger("org.h2").asInstanceOf[Logger]
    h2Logger.setLevel(Level.ERROR)

  val h2DataSourceLayer: ZLayer[Any, Throwable, DataSource] =
    ZLayer.scoped:
      ZIO.acquireRelease {
        ZIO.attempt:
          val config = new HikariConfig()
          val dbName = s"test_db_${UUID.randomUUID()}"
          config.setJdbcUrl(s"jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
          config.setDriverClassName("org.h2.Driver")
          config.setUsername("sa")
          config.setPassword("")
          val ds = new HikariDataSource(config)

          val conn = ds.getConnection()
          conn.createStatement().execute("DROP TABLE IF EXISTS users")
          conn
            .createStatement()
            .execute:
              """CREATE TABLE users (
              id VARCHAR(255) PRIMARY KEY,
              email VARCHAR(255),
              password_hash VARCHAR(255),
              first_name VARCHAR(255),
              last_name VARCHAR(255),
              is_active BOOLEAN,
              created_at TIMESTAMP,
              updated_at TIMESTAMP
            )"""
          conn.close()
          ds
      }(ds => ZIO.attemptBlocking(ds.close()).orDie)

  class MockUserEntityMapper extends UserEntityMapper:
    override def toUser(userEntity: UserEntity): Task[User] =
      ZIO.attempt:
        User(
          id = unsafeUserId(userEntity.id),
          email = unsafeEmail(userEntity.email),
          passwordHash = userEntity.passwordHash,
          firstName = userEntity.firstName.map(unsafeFirstName),
          lastName = userEntity.lastName.map(unsafeLastName),
          isActive = userEntity.isActive,
        )
    override def fromUser(
      user: User,
      createdAt: Instant,
      updatedAt: Instant,
    ): Task[UserEntity] =
      ZIO.succeed:
        UserEntity(
          id = user.id.value,
          email = user.email.value,
          passwordHash = user.passwordHash,
          firstName = user.firstName.map(_.value),
          lastName = user.lastName.map(_.value),
          isActive = user.isActive,
          createdAt = createdAt,
          updatedAt = updatedAt,
        )

    override def createUserEntity(
      id: UserId,
      email: String,
      passwordHash: String,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[UserEntity] =
      ZIO.succeed:
        UserEntity(
          id = id.value,
          email = email,
          passwordHash = passwordHash,
          firstName = firstName,
          lastName = lastName,
          isActive = true,
          createdAt = Instant.now(),
          updatedAt = Instant.now(),
        )

  val quillLayer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  val mapperLayer: ZLayer[Any, Nothing, UserEntityMapper] =
    ZLayer.succeed(new MockUserEntityMapper)

  val commonDependenciesLayer: ZLayer[Any, Throwable, Quill.Postgres[SnakeCase]] =
    h2DataSourceLayer >>> quillLayer

  val userRepoLayer: ZLayer[Quill.Postgres[SnakeCase] & UserEntityMapper, Throwable, UserRepository] =
    UserRepositoryImpl.layer

  val testEnvLayer: ZLayer[Any, Throwable, UserRepository & Quill.Postgres[SnakeCase] & UserEntityMapper] =
    (commonDependenciesLayer ++ mapperLayer) >>> (userRepoLayer ++ ZLayer
      .environment[Quill.Postgres[SnakeCase] & UserEntityMapper])

  def spec =
    suite("UserRepository")(
      test("findById should return None for non-existent user") {
        for
          repo <- ZIO.service[UserRepository]
          user <- repo.findById(unsafeUserId("non-existent"))
        yield assertTrue(user.isEmpty)
      },
      test("create and findById should work correctly") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create("test@example.com", "hashedPassword", Some("Test"), Some("User"))
          foundUserOpt <- repo.findById(createdUser.id)
        yield assertTrue {
          foundUserOpt.isDefined &&
          foundUserOpt.get.id.equals(createdUser.id) &&
          foundUserOpt.get.email.value == "test@example.com" &&
          foundUserOpt.get.firstName.map(_.value) == Some("Test") &&
          foundUserOpt.get.lastName.map(_.value) == Some("User")
        }
      },
      test("findByEmail should return user by email") {
        for
          repo <- ZIO.service[UserRepository]
          _ <- repo.create("email_test@example.com", "password", Some("Email"), Some("Test"))
          userOpt <- repo.findByEmail("email_test@example.com")
        yield assertTrue {
          userOpt.isDefined &&
          userOpt.get.email.value == "email_test@example.com" &&
          userOpt.get.firstName.map(_.value) == Some("Email") &&
          userOpt.get.lastName.map(_.value) == Some("Test")
        }
      },
      test("update should modify user properties") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create("update@example.com", "password", Some("Before"), Some("Update"))
          updatedUserOpt <- repo.update(createdUser.id, Some("After"), Some("Updated"))
          retrievedUser <- repo.findById(createdUser.id)
        yield assertTrue {
          updatedUserOpt.isDefined &&
          updatedUserOpt.get.firstName.map(_.value) == Some("After") &&
          updatedUserOpt.get.lastName.map(_.value) == Some("Updated") &&
          retrievedUser.isDefined &&
          retrievedUser.get.firstName.map(_.value) == Some("After") &&
          retrievedUser.get.lastName.map(_.value) == Some("Updated")
        }
      },
      test("updatePassword should change password") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create("password@example.com", "oldPassword", Some("Password"), Some("Test"))
          result <- repo.updatePassword(createdUser.id, "newPassword")
          updatedUser <- repo.findById(createdUser.id)
        yield assertTrue(
          result &&
          updatedUser.isDefined &&
          updatedUser.get.passwordHash == "newPassword"
        )
      },
      test("deactivate should set isActive to false") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create("deactivate@example.com", "password", Some("Deactivate"), Some("Test"))
          result <- repo.deactivate(createdUser.id)
          updatedUser <- repo.findById(createdUser.id)
        yield assertTrue(
          result &&
          updatedUser.isDefined &&
          !updatedUser.get.isActive
        )
      },
      test("operations should return appropriate results for non-existent users") {
        for
          repo <- ZIO.service[UserRepository]
          nonExistentId = unsafeUserId("does-not-exist")
          updateResult <- repo.update(nonExistentId, Some("First"), Some("Last"))
          passwordResult <- repo.updatePassword(nonExistentId, "newPassword")
          deactivateResult <- repo.deactivate(nonExistentId)
        yield assertTrue(
          updateResult.isEmpty &&
          !passwordResult &&
          !deactivateResult
        )
      },
    ).provide(testEnvLayer)
