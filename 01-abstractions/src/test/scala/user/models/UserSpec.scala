package user.models

import zio.*
import zio.test.*
import zio.json.*

object UserSpec extends ZIOSpecDefault:
  def spec =
    suite("User model")(
      test("User should correctly serialize to JSON and back") {
        val user =
          User(
            id = UserId("test-1234"),
            email = "test@example.com",
            passwordHash = "hashed-password-123",
            firstName = Some("John"),
            lastName = Some("Doe"),
            isActive = true,
          )

        val json = user.toJson
        val parsedUser = json.fromJson[User]

        assertTrue(parsedUser.isRight) &&
        assertTrue(parsedUser.map(_.equals(user)).getOrElse(false))
      },
      test("UserId should correctly serialize to JSON and back") {
        val userId = UserId("user-id-12345")
        val json = userId.toJson
        val parsedId = json.fromJson[UserId]

        assertTrue(parsedId.isRight) &&
        assertTrue(parsedId.map(_.equals(userId)).getOrElse(false)) &&
        assertTrue(parsedId.map(_.value).getOrElse("") == "user-id-12345")
      },
      test("Users with same values should be equal") {
        val user1 =
          User(
            id = UserId("same-id"),
            email = "same@example.com",
            passwordHash = "same-hash",
            firstName = Some("Same"),
            lastName = Some("Name"),
            isActive = true,
          )

        val user2 =
          User(
            id = UserId("same-id"),
            email = "same@example.com",
            passwordHash = "same-hash",
            firstName = Some("Same"),
            lastName = Some("Name"),
            isActive = true,
          )

        assertTrue(user1.id.value == user2.id.value) &&
        assertTrue(user1.email == user2.email) &&
        assertTrue(user1.passwordHash == user2.passwordHash) &&
        assertTrue(user1.firstName == user2.firstName) &&
        assertTrue(user1.lastName == user2.lastName) &&
        assertTrue(user1.isActive == user2.isActive)
      },
      test("Users with different values should not be equal") {
        val user1 =
          User(
            id = UserId("id-1"),
            email = "user1@example.com",
            passwordHash = "hash1",
            firstName = Some("John"),
            lastName = Some("Doe"),
            isActive = true,
          )

        val user2 =
          User(
            id = UserId("id-2"),
            email = "user2@example.com",
            passwordHash = "hash2",
            firstName = Some("Jane"),
            lastName = Some("Smith"),
            isActive = false,
          )

        assertTrue {
          user1.id.value != user2.id.value ||
          user1.email != user2.email ||
          user1.passwordHash != user2.passwordHash ||
          user1.firstName != user2.firstName ||
          user1.lastName != user2.lastName ||
          user1.isActive != user2.isActive
        }
      },
    )
