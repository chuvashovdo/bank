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
            id = UserId.random,
            email =
              Email("test@example.com").getOrElse(
                throw new RuntimeException("Invalid Email in test data")
              ),
            passwordHash = "hashed-password-123",
            firstName =
              Some(FirstName("John").getOrElse(throw new RuntimeException("Invalid FirstName"))),
            lastName =
              Some(LastName("Doe").getOrElse(throw new RuntimeException("Invalid LastName"))),
            isActive = true,
          )

        val json = user.toJson
        val parsedUser = json.fromJson[User]

        assertTrue(parsedUser.isRight) &&
        assertTrue(parsedUser.map(_.equals(user)).getOrElse(false))
      },
      test("UserId should correctly serialize to JSON and back") {
        val userId = UserId.random
        val json = userId.toJson
        val parsedId = json.fromJson[UserId]

        assertTrue(parsedId.isRight) &&
        assertTrue(parsedId.map(_.equals(userId)).getOrElse(false))
      },
      test("Users with same values should be equal") {
        val user1 =
          User(
            id = UserId.random,
            email =
              Email("same@example.com").getOrElse(throw new RuntimeException("Invalid Email")),
            passwordHash = "same-hash",
            firstName =
              Some(FirstName("Same").getOrElse(throw new RuntimeException("Invalid FirstName"))),
            lastName =
              Some(LastName("Name").getOrElse(throw new RuntimeException("Invalid LastName"))),
            isActive = true,
          )

        val user2 =
          User(
            id = user1.id,
            email =
              Email("same@example.com").getOrElse(throw new RuntimeException("Invalid Email")),
            passwordHash = "same-hash",
            firstName =
              Some(FirstName("Same").getOrElse(throw new RuntimeException("Invalid FirstName"))),
            lastName =
              Some(LastName("Name").getOrElse(throw new RuntimeException("Invalid LastName"))),
            isActive = true,
          )

        assertTrue(user1.equals(user2)) &&
        assertTrue(user1.id.equals(user2.id)) &&
        assertTrue(user1.email.value == user2.email.value) &&
        assertTrue(user1.firstName.map(_.value) == user2.firstName.map(_.value)) &&
        assertTrue(user1.lastName.map(_.value) == user2.lastName.map(_.value))
      },
      test("Users with different values should not be equal") {
        val user1 =
          User(
            id = UserId.random,
            email =
              Email("user1@example.com").getOrElse(throw new RuntimeException("Invalid Email")),
            passwordHash = "hash1",
            firstName =
              Some(FirstName("John").getOrElse(throw new RuntimeException("Invalid FirstName"))),
            lastName =
              Some(LastName("Doe").getOrElse(throw new RuntimeException("Invalid LastName"))),
            isActive = true,
          )

        val user2 =
          User(
            id = UserId.random,
            email =
              Email("user2@example.com").getOrElse(throw new RuntimeException("Invalid Email")),
            passwordHash = "hash2",
            firstName =
              Some(FirstName("Jane").getOrElse(throw new RuntimeException("Invalid FirstName"))),
            lastName =
              Some(LastName("Smith").getOrElse(throw new RuntimeException("Invalid LastName"))),
            isActive = false,
          )
        assertTrue(!user1.equals(user2))
      },
    )
