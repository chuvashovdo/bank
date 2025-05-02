package user.mapper

import zio.*
import zio.test.*
import user.models.User
import user.models.UserId
import user.models.UserResponse

object UserResponseMapperSpec extends ZIOSpecDefault:
  val testUser =
    User(
      id = UserId("test-id"),
      email = "test@example.com",
      passwordHash = "hashed-password",
      firstName = Some("Test"),
      lastName = Some("User"),
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
          response.email == "test@example.com",
          response.firstName == Some("Test"),
          response.lastName == Some("User"),
        )
      },
      test("fromUser should handle empty firstName and lastName") {
        val userWithoutNames =
          User(
            id = UserId("no-names"),
            email = "nonames@example.com",
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
          response.email == "nonames@example.com",
          response.firstName.isEmpty,
          response.lastName.isEmpty,
        )
      },
    ).provide(UserResponseMapperImpl.layer)
