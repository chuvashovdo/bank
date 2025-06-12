package user.repository

import zio.*
import zio.test.*
import user.models.User
import user.models.UserId
import com.zaxxer.hikari.*
import javax.sql.DataSource
import io.getquill.*
import io.getquill.jdbczio.Quill
import java.util.UUID
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{ Level, Logger }
import user.models.{ Email, FirstName, LastName }

object UserRepositorySpec extends ZIOSpecDefault:

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

  val quillLayer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  val commonDependenciesLayer: ZLayer[Any, Throwable, Quill.Postgres[SnakeCase]] =
    h2DataSourceLayer >>> quillLayer

  val userRepoLayer: ZLayer[Quill.Postgres[SnakeCase], Throwable, UserRepository] =
    UserRepositoryImpl.layer

  val testEnvLayer: ZLayer[Any, Throwable, UserRepository & Quill.Postgres[SnakeCase]] =
    commonDependenciesLayer >+> userRepoLayer

  def spec =
    suite("UserRepository")(
      test("findById should return None for non-existent user") {
        for
          repo <- ZIO.service[UserRepository]
          user <- repo.findById(UUID.randomUUID()).exit
        yield assertTrue(user.isFailure)
      },
      test("create and findById should work correctly") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create(
              UUID.randomUUID(),
              "test@example.com",
              "hashedPassword",
              Some("Test"),
              Some("User"),
            )
          foundUser <- repo.findById(createdUser.id.value)
        yield assertTrue {
          foundUser.id.equals(createdUser.id) &&
          foundUser.email.value == "test@example.com" &&
          foundUser.firstName.map(_.value) == Some("Test") &&
          foundUser.lastName.map(_.value) == Some("User")
        }
      },
      test("findByEmail should return user by email") {
        for
          repo <- ZIO.service[UserRepository]
          _ <-
            repo.create(
              UUID.randomUUID(),
              "email_test@example.com",
              "password",
              Some("Email"),
              Some("Test"),
            )
          user <- repo.findByEmail("email_test@example.com")
        yield assertTrue {
          user.email.value == "email_test@example.com" &&
          user.firstName.map(_.value) == Some("Email") &&
          user.lastName.map(_.value) == Some("Test")
        }
      },
      test("update should modify user properties") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create(
              UUID.randomUUID(),
              "update@example.com",
              "password",
              Some("Before"),
              Some("Update"),
            )
          updatedUser <- repo.update(createdUser.id.value, Some("After"), Some("Updated"))
          retrievedUser <- repo.findById(createdUser.id.value)
        yield assertTrue {
          updatedUser.firstName.map(_.value) == Some("After") &&
          updatedUser.lastName.map(_.value) == Some("Updated") &&
          retrievedUser.firstName.map(_.value) == Some("After") &&
          retrievedUser.lastName.map(_.value) == Some("Updated")
        }
      },
      test("updatePassword should change password") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create(
              UUID.randomUUID(),
              "password@example.com",
              "oldPassword",
              Some("Password"),
              Some("Test"),
            )
          result <- repo.updatePassword(createdUser.id.value, "newPassword")
          updatedUser <- repo.findById(createdUser.id.value)
        yield assertTrue {
          updatedUser.passwordHash == "newPassword"
        }
      },
      test("deactivate should set isActive to false") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create(
              UUID.randomUUID(),
              "deactivate@example.com",
              "password",
              Some("Deactivate"),
              Some("Test"),
            )
          result <- repo.deactivate(createdUser.id.value)
          updatedUser <- repo.findById(createdUser.id.value)
        yield assertTrue {
          !updatedUser.isActive
        }
      },
      test("operations should return appropriate results for non-existent users") {
        for
          repo <- ZIO.service[UserRepository]
          nonExistentId = UUID.randomUUID()
          updateResult <- repo.update(nonExistentId, Some("First"), Some("Last")).exit
          passwordResult <- repo.updatePassword(nonExistentId, "newPassword").exit
          deactivateResult <- repo.deactivate(nonExistentId).exit
        yield assertTrue {
          updateResult.isFailure &&
          passwordResult.isFailure &&
          deactivateResult.isFailure
        }
      },
    ).provide(testEnvLayer)
