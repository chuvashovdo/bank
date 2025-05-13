package user.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import user.models.*
import user.repository.*
import user.service.*
import java.util.UUID
import common.errors.*

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

  private def hashPassword(password: String): String =
    s"hashed_${password}_test"

  private val password1Hash =
    hashPassword("password")
  private val password2Hash =
    hashPassword("password2")

  class MockUserRepository extends UserRepository:
    private var users: Map[UserId, User] =
      Map(
        unsafeUserId("1") -> User(
          unsafeUserId("1"),
          unsafeEmail("test@test.com"),
          password1Hash,
          Some(unsafeFirstName("Test")),
          Some(unsafeLastName("User")),
          isActive = true,
        ),
        unsafeUserId("2") -> User(
          unsafeUserId("2"),
          unsafeEmail("test2@test.com"),
          password2Hash,
          Some(unsafeFirstName("Test2")),
          Some(unsafeLastName("User2")),
          isActive = true,
        ),
        unsafeUserId("3") -> User(
          unsafeUserId("3"),
          unsafeEmail("testdeactivate@test.com"),
          password1Hash,
          Some(unsafeFirstName("Test")),
          Some(unsafeLastName("User")),
          isActive = false,
        ),
      )

    val nonExistentId =
      unsafeUserId("999")

    override def findById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.get(id))

    override def findByEmail(email: String): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email.value == email))

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
      id: UserId,
      firstNameStr: Option[String],
      lastNameStr: Option[String],
    ): Task[Option[User]] =
      users.get(id).fold(ZIO.succeed(None)) { user =>
        if !user.isActive then ZIO.succeed(None)
        else
          val updatedUser =
            user.copy(
              firstName = firstNameStr.map(unsafeFirstName),
              lastName = lastNameStr.map(unsafeLastName),
            )
          users += (id -> updatedUser)
          ZIO.succeed(Some(updatedUser))
      }

    override def updatePassword(id: UserId, passwordHash: String): Task[Boolean] =
      users.get(id).fold(ZIO.succeed(false)) { user =>
        if !user.isActive then ZIO.succeed(false)
        else
          val updatedUser = user.copy(passwordHash = passwordHash)
          users += (id -> updatedUser)
          ZIO.succeed(true)
      }

    override def deactivate(id: UserId): Task[Boolean] =
      users.get(id).fold(ZIO.succeed(false)) { user =>
        val updatedUser = user.copy(isActive = false)
        users += (id -> updatedUser)
        ZIO.succeed(true)
      }

  val mockUserRepositoryLayer: ULayer[UserRepository] =
    ZLayer.succeed(new MockUserRepository)

  class MockUserService(userRepository: UserRepository) extends UserService:
    override def findUserById(id: UserId): Task[Option[User]] =
      userRepository.findById(id)

    override def findUserByEmail(email: Email): Task[Option[User]] =
      userRepository.findByEmail(email.value)

    override def registerUser(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      for
        existingUser <- userRepository.findByEmail(email.value)
        _ <- ZIO.fail(UserAlreadyExistsError(email)).when(existingUser.isDefined)
        passwordHash = hashPassword(password.value) // Используем наш упрощенный метод хеширования
        user <-
          userRepository.create(
            email = email.value,
            passwordHash = passwordHash,
            firstName = firstName.map(_.value),
            lastName = lastName.map(_.value),
          )
      yield user

    override def validateCredentials(email: Email, password: Password): Task[Option[User]] =
      for
        userOpt <- userRepository.findByEmail(email.value)
        user <- ZIO.fromOption(userOpt).orElseFail(InvalidCredentialsError())
        _ <- ZIO.fail(UserNotActiveError(email.value)).when(!user.isActive)
        // Проверяем пароль по нашей упрощенной схеме
        isValid = user.passwordHash == hashPassword(password.value)
        _ <- ZIO.fail(InvalidCredentialsError()).when(!isValid)
      yield Some(user)

    override def updateUser(
      id: UserId,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[Option[User]] =
      userRepository.update(id, firstName.map(_.value), lastName.map(_.value))

    override def changePassword(
      id: UserId,
      oldPassword: Password,
      newPassword: Password,
    ): Task[Boolean] =
      for
        userOpt <- userRepository.findById(id)
        user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(id.value))
        _ <- ZIO.fail(UserNotActiveError(id.value)).when(!user.isActive)
        // Проверяем пароль по нашей упрощенной схеме
        isValid = user.passwordHash == hashPassword(oldPassword.value)
        _ <- ZIO.fail(InvalidOldPasswordError(id)).when(!isValid)
        newHash = hashPassword(newPassword.value)
        updated <- userRepository.updatePassword(id, newHash)
      yield updated

    override def deactivateUser(id: UserId): Task[Boolean] =
      for
        userOpt <- userRepository.findById(id)
        _ <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(id.value))
        deactivated <- userRepository.deactivate(id)
      yield deactivated

  val mockUserServiceLayer: ZLayer[UserRepository, Nothing, UserService] =
    ZLayer:
      for userRepository <- ZIO.service[UserRepository]
      yield new MockUserService(userRepository)

  val userServiceLayer: ZLayer[Any, Nothing, UserService & UserRepository] =
    ZLayer
      .make[UserService & UserRepository](
        mockUserServiceLayer,
        mockUserRepositoryLayer,
      )

  def spec =
    suite("UserService")(
      test("findById should return user by id") {
        for
          userService <- ZIO.service[UserService]
          userIdToFind = unsafeUserId("1")
          userOpt <- userService.findUserById(userIdToFind)
        yield assertTrue(userOpt.isDefined && userOpt.get.id.equals(userIdToFind))
      }.provide(userServiceLayer),
      test("findByEmail should return user by email") {
        for
          userService <- ZIO.service[UserService]
          emailToFind = unsafeEmail("test@test.com")
          userOpt <- userService.findUserByEmail(emailToFind)
        yield assertTrue(userOpt.isDefined && userOpt.get.email.equals(emailToFind))
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
          userOpt <- userService.validateCredentials(email, password)
        yield assertTrue(userOpt.isDefined && userOpt.get.email.equals(email))
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
          updatedUserOpt <- userService.updateUser(userIdToUpdate, newFirstName, newLastName)
        yield assertTrue(updatedUserOpt.isDefined) &&
        assertTrue(updatedUserOpt.get.firstName.map(_.value) == newFirstName.map(_.value)) &&
        assertTrue(updatedUserOpt.get.lastName.map(_.value) == newLastName.map(_.value))
      }.provide(userServiceLayer),
      test("updateUser should return None for deactivated user") {
        for
          userService <- ZIO.service[UserService]
          userIdToUpdate = unsafeUserId("3")
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          userOpt <- userService.updateUser(userIdToUpdate, newFirstName, newLastName)
        yield assertTrue(userOpt.isEmpty)
      }.provide(userServiceLayer),
      test("updateUser should return None for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          nonExistentUserId = repo.asInstanceOf[MockUserRepository].nonExistentId
          newFirstName = Some(valid(FirstName("New"), "FirstName"))
          newLastName = Some(valid(LastName("UserUpdated"), "LastName"))
          userOpt <- userService.updateUser(nonExistentUserId, newFirstName, newLastName)
        yield assertTrue(userOpt.isEmpty)
      }.provide(userServiceLayer),
      test("changePassword should change password") {
        for
          userService <- ZIO.service[UserService]
          userIdToChange = unsafeUserId("1")
          oldPassword = valid(Password("password"), "OldPassword")
          newPassword = valid(Password("newpassword"), "NewPassword")
          result <- userService.changePassword(userIdToChange, oldPassword, newPassword)
        yield assertTrue(result)
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
          result <- userService.deactivateUser(userIdToDeactivate)
        yield assertTrue(result)
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
