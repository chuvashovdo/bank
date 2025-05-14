package user.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import user.models.*
import user.repository.*
import user.service.*
import java.util.UUID
import common.errors.*
import org.mindrot.jbcrypt.BCrypt

object UserServiceSpec extends ZIOSpecDefault:
  private def valid[E <: Throwable, A](either: Either[E, A], fieldName: String): A =
    either.fold(
      e =>
        throw new RuntimeException(s"Failed to create valid $fieldName for test: ${e.getMessage}"),
      identity,
    )

  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException("Invalid UserId in mock setup"))
  private def unsafeEmail(email: String): Email =
    Email(email).getOrElse(throw new RuntimeException("Invalid Email in mock setup"))
  private def unsafeFirstName(name: String): FirstName =
    FirstName(name).getOrElse(throw new RuntimeException("Invalid FirstName in mock setup"))
  private def unsafeLastName(name: String): LastName =
    LastName(name).getOrElse(throw new RuntimeException("Invalid LastName in mock setup"))

  private val passwordUser1Hashed: String =
    BCrypt.hashpw("password", BCrypt.gensalt(4))
  private val passwordUser2Hashed: String =
    BCrypt.hashpw("password2", BCrypt.gensalt(4))
  private val passwordUser3Hashed: String =
    BCrypt.hashpw("password", BCrypt.gensalt(4))

  class MockUserRepository extends UserRepository:
    private var users: Map[UserId, User] =
      Map(
        unsafeUserId("1") -> User(
          unsafeUserId("1"),
          unsafeEmail("test@test.com"),
          passwordUser1Hashed,
          Some(unsafeFirstName("Test")),
          Some(unsafeLastName("User")),
          isActive = true,
        ),
        unsafeUserId("2") -> User(
          unsafeUserId("2"),
          unsafeEmail("test2@test.com"),
          passwordUser2Hashed,
          Some(unsafeFirstName("Test2")),
          Some(unsafeLastName("User2")),
          isActive = true,
        ),
        unsafeUserId("3") -> User(
          unsafeUserId("3"),
          unsafeEmail("testdeactivate@test.com"),
          passwordUser3Hashed,
          Some(unsafeFirstName("Test")),
          Some(unsafeLastName("User")),
          isActive = false,
        ),
      )

    val nonExistentId =
      unsafeUserId("999")

    override def findById(id: String): Task[User] =
      ZIO.attempt(users.get(unsafeUserId(id)).get).mapError(_ => new UserNotFoundError(id))

    override def findByEmail(email: String): Task[User] =
      ZIO
        .attempt(users.values.find(_.email.value == email).get)
        .mapError(_ => new UserNotFoundError(email))

    override def create(
      emailStr: String,
      passwordHash: String,
      firstNameStr: Option[String],
      lastNameStr: Option[String],
    ): Task[User] =
      val id = unsafeUserId(UUID.randomUUID().toString)
      val newEmail = unsafeEmail(emailStr)
      val newFirstName = firstNameStr.map(unsafeFirstName)
      val newLastName = lastNameStr.map(unsafeLastName)
      val newUser = User(id, newEmail, passwordHash, newFirstName, newLastName, isActive = true)
      users += (id -> newUser)
      ZIO.succeed(newUser)

    override def update(
      id: String,
      firstNameStr: Option[String],
      lastNameStr: Option[String],
    ): Task[User] =
      users.get(unsafeUserId(id)).fold(ZIO.fail(new UserNotFoundError(id))) { user =>
        if !user.isActive then ZIO.fail(new UserNotActiveError(id))
        else
          val updatedUser =
            user.copy(
              firstName = firstNameStr.map(unsafeFirstName),
              lastName = lastNameStr.map(unsafeLastName),
            )
          users += (unsafeUserId(id) -> updatedUser)
          ZIO.succeed(updatedUser)
      }

    override def updatePassword(id: String, passwordHash: String): Task[Unit] =
      users.get(unsafeUserId(id)).fold(ZIO.fail(new UserNotFoundError(id))) { user =>
        if !user.isActive then ZIO.fail(new UserNotActiveError(id))
        else
          val updatedUser = user.copy(passwordHash = passwordHash)
          users += (unsafeUserId(id) -> updatedUser)
          ZIO.succeed(())
      }

    override def deactivate(id: String): Task[Unit] =
      users.get(unsafeUserId(id)).fold(ZIO.fail(new UserNotFoundError(id))) { user =>
        val updatedUser = user.copy(isActive = false)
        users += (unsafeUserId(id) -> updatedUser)
        ZIO.succeed(())
      }

  val mockUserRepositoryLayer: ULayer[UserRepository] =
    ZLayer.succeed(new MockUserRepository)

  val userServiceLayer: ZLayer[Any, Nothing, UserService & UserRepository] =
    ZLayer.make[UserService & UserRepository](
      UserServiceImpl.layer,
      mockUserRepositoryLayer,
    )

  def spec =
    suite("UserService")(
      test("findById should return user by id") {
        for
          userService <- ZIO.service[UserService]
          userIdToFind = unsafeUserId("1")
          user <- userService.findUserById(userIdToFind)
        yield assertTrue(user.id.equals(userIdToFind))
      }.provide(userServiceLayer),
      test("findByEmail should return user by email") {
        for
          userService <- ZIO.service[UserService]
          emailToFind = unsafeEmail("test@test.com")
          user <- userService.findUserByEmail(emailToFind)
        yield assertTrue(user.email.equals(emailToFind))
      }.provide(userServiceLayer),
      test("registerUser should create user") {
        for
          userService <- ZIO.service[UserService]
          email = valid(Email("test3@test.com"), "Email")
          password = valid(Password("password3"), "Password")
          firstName = Some(valid(FirstName("Test3"), "FirstName"))
          lastName = Some(valid(LastName("User3"), "LastName"))

          createdUser <- userService.registerUser(email, password, firstName, lastName)
        yield assertTrue(createdUser.email.equals(email)) &&
        assertTrue(createdUser.firstName.map(_.value) == firstName.map(_.value)) &&
        assertTrue(createdUser.lastName.map(_.value) == lastName.map(_.value)) &&
        assertTrue(createdUser.passwordHash.nonEmpty)
      }.provide(userServiceLayer),
      test("registerUser should fail for existing user with UserAlreadyExistsError") {
        for
          userService <- ZIO.service[UserService]
          email = unsafeEmail("test@test.com")
          password = valid(Password("password"), "Password")
          firstName = Some(unsafeFirstName("Test"))
          lastName = Some(unsafeLastName("User"))

          result <- userService.registerUser(email, password, firstName, lastName).exit
        yield assert(result)(fails(isSubtype[UserAlreadyExistsError](anything)))
      }.provide(userServiceLayer),
      test("validateCredentials should return user for valid credentials") {
        for
          userService <- ZIO.service[UserService]
          email = unsafeEmail("test@test.com")
          password = valid(Password("password"), "Password")
          user <- userService.validateCredentials(email, password)
        yield assertTrue(user.email.equals(email))
      }.provide(userServiceLayer),
      test("validateCredentials should fail for deactivated user with UserNotActiveError") {
        for
          userService <- ZIO.service[UserService]
          email = unsafeEmail("testdeactivate@test.com")
          password = valid(Password("password"), "Password")
          result <- userService.validateCredentials(email, password).exit
        yield assert(result)(fails(isSubtype[UserNotActiveError](anything)))
      }.provide(userServiceLayer),
      test("validateCredentials should fail for invalid credentials with InvalidCredentialsError") {
        for
          userService <- ZIO.service[UserService]
          email = unsafeEmail("test@test.com")
          password = valid(Password("wrongpassword"), "Password")
          result <- userService.validateCredentials(email, password).exit
        yield assert(result)(fails(isSubtype[InvalidCredentialsError](anything)))
      }.provide(userServiceLayer),
      test("updateUser should update user") {
        for
          userService <- ZIO.service[UserService]
          userIdToUpdate = unsafeUserId("1")
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          updatedUser <- userService.updateUser(userIdToUpdate, newFirstName, newLastName)
        yield assertTrue(updatedUser.firstName.map(_.value) == newFirstName.map(_.value)) &&
        assertTrue(updatedUser.lastName.map(_.value) == newLastName.map(_.value))
      }.provide(userServiceLayer),
      test("updateUser should return None for deactivated user") {
        for
          userService <- ZIO.service[UserService]
          userIdToUpdate = unsafeUserId("3")
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          user <- userService.updateUser(userIdToUpdate, newFirstName, newLastName).exit
        yield assertTrue(user.isFailure)
      }.provide(userServiceLayer),
      test("updateUser should return None for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          nonExistentUserId = repo.asInstanceOf[MockUserRepository].nonExistentId
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          user <- userService.updateUser(nonExistentUserId, newFirstName, newLastName).exit
        yield assertTrue(user.isFailure)
      }.provide(userServiceLayer),
      test("changePassword should change password") {
        for
          userService <- ZIO.service[UserService]
          userIdToChange = unsafeUserId("1")
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(userIdToChange, oldPassword, newPassword).exit
        yield assertTrue(result.isSuccess)
      }.provide(userServiceLayer),
      test("changePassword should fail for deactivated user with UserNotActiveError") {
        for
          userService <- ZIO.service[UserService]
          userIdToChange = unsafeUserId("3")
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(userIdToChange, oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[UserNotActiveError](anything)))
      }.provide(userServiceLayer),
      test("changePassword should fail for non-existent user with UserNotFoundError") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          nonExistentUserId = repo.asInstanceOf[MockUserRepository].nonExistentId
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(nonExistentUserId, oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[UserNotFoundError](anything)))
      }.provide(userServiceLayer),
      test("changePassword should fail for invalid old password with InvalidOldPasswordError") {
        for
          userService <- ZIO.service[UserService]
          userIdToChange = unsafeUserId("1")
          oldPassword = valid(Password("wrongoldpassword"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(userIdToChange, oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[InvalidOldPasswordError](anything)))
      }.provide(userServiceLayer),
      test("deactivateUser should deactivate user") {
        for
          userService <- ZIO.service[UserService]
          userIdToDeactivate = unsafeUserId("1")
          result <- userService.deactivateUser(userIdToDeactivate).exit
        yield assertTrue(result.isSuccess)
      }.provide(userServiceLayer),
      test("deactivateUser should fail for non-existent user with UserNotFoundError") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          nonExistentUserId = repo.asInstanceOf[MockUserRepository].nonExistentId
          result <- userService.deactivateUser(nonExistentUserId).exit
        yield assert(result)(fails(isSubtype[UserNotFoundError](anything)))
      }.provide(userServiceLayer),
      test("validateCredentials should fail after user deactivation with UserNotActiveError") {
        for
          userService <- ZIO.service[UserService]
          userIdToDeactivate = unsafeUserId("2")
          emailToValidate = unsafeEmail("test2@test.com")
          passwordToValidate = valid(Password("password2"), "Password")
          _ <- userService.deactivateUser(userIdToDeactivate)
          result <- userService.validateCredentials(emailToValidate, passwordToValidate).exit
        yield assert(result)(fails(isSubtype[UserNotActiveError](anything)))
      }.provide(userServiceLayer),
    )
