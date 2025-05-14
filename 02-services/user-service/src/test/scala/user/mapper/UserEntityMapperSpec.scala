package user.mapper

import zio.*
import zio.test.*
import user.entity.UserEntity
import user.models.User
import user.models.UserId
import user.models.{ Email, FirstName, LastName }
import java.time.Instant

object UserEntityMapperSpec extends ZIOSpecDefault:
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException(s"Invalid UserId in test setup: $id"))
  private def unsafeEmail(email: String): Email =
    Email(email).getOrElse(throw new RuntimeException(s"Invalid Email in test setup: $email"))
  private def unsafeFirstName(name: String): FirstName =
    FirstName(name).getOrElse(throw new RuntimeException(s"Invalid FirstName in test setup: $name"))
  private def unsafeLastName(name: String): LastName =
    LastName(name).getOrElse(throw new RuntimeException(s"Invalid LastName in test setup: $name"))

  val testUser =
    User(
      id = unsafeUserId("test-id"),
      email = unsafeEmail("test@example.com"),
      passwordHash = "hashed-password",
      firstName = Some(unsafeFirstName("Test")),
      lastName = Some(unsafeLastName("User")),
      isActive = true,
    )

  val testInstant =
    Instant.parse("2025-01-01T12:00:00Z")

  def spec =
    suite("UserEntityMapper")(
      test("toUser should convert UserEntity to User") {
        for
          mapper <- ZIO.service[UserEntityMapper]
          entity =
            UserEntity(
              id = "test-id",
              email = "test@example.com",
              passwordHash = "hashed-password",
              firstName = Some("Test"),
              lastName = Some("User"),
              isActive = true,
              createdAt = testInstant,
              updatedAt = testInstant,
            )
          user <- mapper.toUser(entity)
        yield assertTrue(
          user.id.value == "test-id",
          user.email.value == "test@example.com",
          user.passwordHash == "hashed-password",
          user.firstName.map(_.value) == Some("Test"),
          user.lastName.map(_.value) == Some("User"),
          user.isActive == true,
        )
      },
      test("fromUser should convert User to UserEntity") {
        for
          mapper <- ZIO.service[UserEntityMapper]
          entity <- mapper.fromUser(testUser, testInstant, testInstant)
        yield assertTrue(
          entity.id == "test-id",
          entity.email == "test@example.com",
          entity.passwordHash == "hashed-password",
          entity.firstName == Some("Test"),
          entity.lastName == Some("User"),
          entity.isActive == true,
          entity.createdAt.equals(testInstant),
          entity.updatedAt.equals(testInstant),
        )
      },
      test("createUserEntity should create a new UserEntity") {
        for
          mapper <- ZIO.service[UserEntityMapper]
          entity <-
            mapper.createUserEntity(
              unsafeUserId("new-user"),
              "new@example.com",
              "new-password-hash",
              Some("New"),
              Some("User"),
            )
        yield assertTrue(
          entity.id == "new-user",
          entity.email == "new@example.com",
          entity.passwordHash == "new-password-hash",
          entity.firstName == Some("New"),
          entity.lastName == Some("User"),
          entity.isActive == true,
          entity.createdAt != null,
          entity.updatedAt != null,
        )
      },
    ).provide(UserEntityMapperImpl.layer)
