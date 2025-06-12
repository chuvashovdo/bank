package user.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import user.models.*
import user.repository.*
import user.service.*
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt
import user.errors.*
import user.entity.UserEntity
import java.time.Instant

object UserServiceSpec extends ZIOSpecDefault:
  private def valid[E <: Throwable, A](either: Either[E, A], fieldName: String): A =
    either.fold(
      e =>
        throw new RuntimeException(s"Failed to create valid $fieldName for test: ${e.getMessage}"),
      identity,
    )

  private val userId1 =
    UUID.randomUUID()
  private val userId2 =
    UUID.randomUUID()
  private val userId3 =
    UUID.randomUUID()
  private val nonExistentId =
    UUID.randomUUID()

  private def unsafeUserId(id: UUID): UserId =
    UserId(id)
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
    private var users: Map[UUID, UserEntity] =
      val now = Instant.now()
      Map(
        userId1 -> UserEntity(
          userId1,
          "test@test.com",
          passwordUser1Hashed,
          Some("Test"),
          Some("User"),
          isActive = true,
          now,
          now,
        ),
        userId2 -> UserEntity(
          userId2,
          "test2@test.com",
          passwordUser2Hashed,
          Some("Test2"),
          Some("User2"),
          isActive = true,
          now,
          now,
        ),
        userId3 -> UserEntity(
          userId3,
          "testdeactivate@test.com",
          passwordUser3Hashed,
          Some("Test"),
          Some("User"),
          isActive = false,
          now,
          now,
        ),
      )

    override def findById(id: UUID): Task[UserEntity] =
      users.get(id) match
        case Some(user) => ZIO.succeed(user)
        case None => ZIO.fail(UserNotFoundError(id))

    override def findByEmail(email: String): Task[UserEntity] =
      users.values.find(_.email == email) match
        case Some(user) => ZIO.succeed(user)
        case None => ZIO.fail(UserNotFoundError(email))

    override def create(user: UserEntity): Task[UserEntity] =
      ZIO.succeed:
        users = users + (user.id -> user)
        user

    override def update(user: UserEntity): Task[UserEntity] =
      users.get(user.id) match
        case Some(_) =>
          ZIO.succeed:
            users = users + (user.id -> user)
            user
        case None => ZIO.fail(UserNotFoundError(user.id))

    override def updatePassword(id: UUID, passwordHash: String): Task[Unit] =
      users.get(id) match
        case Some(user) =>
          val updatedUser = user.copy(passwordHash = passwordHash, updatedAt = Instant.now())
          users = users + (id -> updatedUser)
          ZIO.unit
        case None => ZIO.fail(UserNotFoundError(id))

    override def deactivate(id: UUID): Task[Unit] =
      users.get(id) match
        case Some(user) =>
          val updatedUser = user.copy(isActive = false, updatedAt = Instant.now())
          users = users + (id -> updatedUser)
          ZIO.unit
        case None => ZIO.fail(UserNotFoundError(id))

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
          user <- userService.findUserById(unsafeUserId(userId1))
        yield assertTrue(user.id.equals(unsafeUserId(userId1)))
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
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          updatedUser <- userService.updateUser(unsafeUserId(userId1), newFirstName, newLastName)
        yield assertTrue(updatedUser.firstName.map(_.value) == newFirstName.map(_.value)) &&
        assertTrue(updatedUser.lastName.map(_.value) == newLastName.map(_.value))
      }.provide(userServiceLayer),
      test("updateUser should fail for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          user <-
            userService.updateUser(unsafeUserId(nonExistentId), newFirstName, newLastName).exit
        yield assert(user)(fails(isSubtype[UserNotFoundError](anything)))
      }.provide(userServiceLayer),
      test("changePassword should change password") {
        for
          userService <- ZIO.service[UserService]
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(unsafeUserId(userId1), oldPassword, newPassword).exit
        yield assertTrue(result.isSuccess)
      }.provide(userServiceLayer),
      test("changePassword should fail for deactivated user with UserNotActiveError") {
        for
          userService <- ZIO.service[UserService]
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(unsafeUserId(userId3), oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[UserNotActiveError](anything)))
      }.provide(userServiceLayer),
      test("changePassword should fail for non-existent user with UserNotFoundError") {
        for
          userService <- ZIO.service[UserService]
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <-
            userService.changePassword(unsafeUserId(nonExistentId), oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[UserNotFoundError](anything)))
      }.provide(userServiceLayer),
      test("changePassword should fail for invalid old password with InvalidOldPasswordError") {
        for
          userService <- ZIO.service[UserService]
          oldPassword = valid(Password("wrongoldpassword"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(unsafeUserId(userId1), oldPassword, newPassword).exit
        yield assert(result)(fails(isSubtype[InvalidOldPasswordError](anything)))
      }.provide(userServiceLayer),
      test("deactivateUser should deactivate user") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.deactivateUser(unsafeUserId(userId1)).exit
        yield assertTrue(result.isSuccess)
      }.provide(userServiceLayer),
      test("deactivateUser should fail for non-existent user with UserNotFoundError") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.deactivateUser(unsafeUserId(nonExistentId)).exit
        yield assert(result)(fails(isSubtype[UserNotFoundError](anything)))
      }.provide(userServiceLayer),
      test("validateCredentials should fail after user deactivation with UserNotActiveError") {
        for
          userService <- ZIO.service[UserService]
          _ <- userService.deactivateUser(unsafeUserId(userId2))
          emailToValidate = unsafeEmail("test2@test.com")
          passwordToValidate = valid(Password("password2"), "Password")
          result <- userService.validateCredentials(emailToValidate, passwordToValidate).exit
        yield assert(result)(fails(isSubtype[UserNotActiveError](anything)))
      }.provide(userServiceLayer),
    )
