package user.mapper

import zio.*
import zio.test.*
import user.models.User
import user.models.UserId
import user.models.UserResponse
import user.models.{ Email, FirstName, LastName }

object UserResponseMapperSpec extends ZIOSpecDefault:
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

  def spec =
    suite("UserResponseMapper")(
      test("fromUser should convert User to UserResponse") {
        for
          mapper <- ZIO.service[UserResponseMapper]
          response <- mapper.fromUser(testUser)
        yield assertTrue(
          response.id == "test-id",
          response.email.value == "test@example.com",
          response.firstName.map(_.value) == Some("Test"),
          response.lastName.map(_.value) == Some("User"),
        )
      },
      test("fromUser should handle empty firstName and lastName") {
        val userWithoutNames =
          User(
            id = unsafeUserId("no-names"),
            email = unsafeEmail("nonames@example.com"),
            passwordHash = "password-hash",
            firstName = None,
            lastName = None,
            isActive = true,
          )

        for
          mapper <- ZIO.service[UserResponseMapper]
          response <- mapper.fromUser(userWithoutNames)
        yield assertTrue(
          response.id == "no-names",
          response.email.value == "nonames@example.com",
          response.firstName.isEmpty,
          response.lastName.isEmpty,
        )
      },
    ).provide(UserResponseMapperImpl.layer)
