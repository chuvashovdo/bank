package user.repository

import zio.*
import zio.test.*
import com.zaxxer.hikari.*
import javax.sql.DataSource
import io.getquill.*
import io.getquill.jdbczio.Quill
import java.util.UUID
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{ Level, Logger }
import user.entity.UserEntity
import java.time.Instant

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
            repo.create {
              UserEntity(
                id = UUID.randomUUID(),
                email = "test@example.com",
                passwordHash = "hashedPassword",
                firstName = Some("Test"),
                lastName = Some("User"),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
              )
            }
          foundUser <- repo.findById(createdUser.id)
        yield assertTrue {
          foundUser.id.equals(createdUser.id) &&
          foundUser.email == "test@example.com" &&
          foundUser.firstName == Some("Test") &&
          foundUser.lastName == Some("User")
        }
      },
      test("findByEmail should return user by email") {
        for
          repo <- ZIO.service[UserRepository]
          _ <-
            repo.create {
              UserEntity(
                id = UUID.randomUUID(),
                email = "email_test@example.com",
                passwordHash = "password",
                firstName = Some("Email"),
                lastName = Some("Test"),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
              )
            }
          user <- repo.findByEmail("email_test@example.com")
        yield assertTrue {
          user.email == "email_test@example.com" &&
          user.firstName == Some("Email") &&
          user.lastName == Some("Test")
        }
      },
      test("update should modify user properties") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create {
              UserEntity(
                id = UUID.randomUUID(),
                email = "update@example.com",
                passwordHash = "password",
                firstName = Some("Before"),
                lastName = Some("Update"),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
              )
            }
          updatedUser <-
            repo.update {
              UserEntity(
                id = createdUser.id,
                email = createdUser.email,
                passwordHash = createdUser.passwordHash,
                firstName = Some("After"),
                lastName = Some("Updated"),
                isActive = createdUser.isActive,
                createdAt = createdUser.createdAt,
                updatedAt = Instant.now(),
              )
            }
          retrievedUser <- repo.findById(createdUser.id)
        yield assertTrue {
          updatedUser.firstName == Some("After") &&
          updatedUser.lastName == Some("Updated") &&
          retrievedUser.firstName == Some("After") &&
          retrievedUser.lastName == Some("Updated")
        }
      },
      test("updatePassword should change password") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create {
              UserEntity(
                id = UUID.randomUUID(),
                email = "password@example.com",
                passwordHash = "oldPassword",
                firstName = Some("Password"),
                lastName = Some("Test"),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
              )
            }
          result <- repo.updatePassword(createdUser.id, "newPassword")
          updatedUser <- repo.findById(createdUser.id)
        yield assertTrue {
          updatedUser.passwordHash == "newPassword"
        }
      },
      test("deactivate should set isActive to false") {
        for
          repo <- ZIO.service[UserRepository]
          createdUser <-
            repo.create {
              UserEntity(
                id = UUID.randomUUID(),
                email = "deactivate@example.com",
                passwordHash = "password",
                firstName = Some("Deactivate"),
                lastName = Some("Test"),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
              )
            }
          result <- repo.deactivate(createdUser.id)
          updatedUser <- repo.findById(createdUser.id)
        yield assertTrue {
          !updatedUser.isActive
        }
      },
    ).provide(testEnvLayer)
