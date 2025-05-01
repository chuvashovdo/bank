package user.service

import zio.*
import zio.test.*
import user.models.*
import user.repository.*
import user.service.*
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

object UserServiceSpec extends ZIOSpecDefault:
  private def hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt(12))

  private val password1Hash =
    hashPassword("password")
  private val password2Hash =
    hashPassword("password2")

  class MockUserRepository extends UserRepository:
    private var users: Map[UserId, User] =
      Map(
        UserId("1") -> User(
          UserId("1"),
          "test@test.com",
          password1Hash,
          Some("Test"),
          Some("User"),
          isActive = true,
        ),
        UserId("2") -> User(
          UserId("2"),
          "test2@test.com",
          password2Hash,
          Some("Test2"),
          Some("User2"),
          isActive = true,
        ),
        UserId("3") -> User(
          UserId("3"),
          "testdeactivate@test.com",
          password1Hash,
          Some("Test"),
          Some("User"),
          isActive = false,
        ),
      )

    // ID для несуществующего пользователя
    val nonExistentId =
      UserId("999")

    override def findById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.get(id))

    override def findByEmail(email: String): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email == email))

    override def create(
      email: String,
      passwordHash: String,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[User] =
      val id = UserId(UUID.randomUUID().toString)
      val newUser = User(id, email, passwordHash, firstName, lastName, isActive = true)
      users += (id -> newUser)
      ZIO.succeed(newUser)

    override def update(
      id: UserId,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[Option[User]] =
      users.get(id).fold(ZIO.succeed(None)) { user =>
        // Проверяем, что пользователь активен
        if !user.isActive then ZIO.succeed(None)
        else
          val updatedUser = user.copy(firstName = firstName, lastName = lastName)
          users += (id -> updatedUser)
          ZIO.succeed(Some(updatedUser))
      }

    override def updatePassword(id: UserId, passwordHash: String): Task[Boolean] =
      users.get(id).fold(ZIO.succeed(false)) { user =>
        // Проверяем, что пользователь активен
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

  val mockUserRepositoryLayer =
    ZLayer.succeed(new MockUserRepository)

  val userServiceLayer =
    ZLayer.make[UserService](UserServiceImpl.layer, mockUserRepositoryLayer)

  def spec =
    suite("UserService")(
      test("findById should return user by id") {
        for
          userService <- ZIO.service[UserService]
          user <- userService.findUserById(UserId("1"))
        yield assertTrue(user.isDefined)
      }.provide(userServiceLayer),
      test("findByEmail should return user by email") {
        for
          userservice <- ZIO.service[UserService]
          user <- userservice.findUserByEmail("test@test.com")
        yield assertTrue(user.isDefined)
      }.provide(userServiceLayer),
      test("registerUser should create user") {
        for
          userService <- ZIO.service[UserService]
          user <-
            userService.registerUser("test3@test.com", "password3", Some("Test3"), Some("User3"))
        yield assertTrue(user.id.value.nonEmpty)
      }.provide(userServiceLayer),
      test("registerUser should fail for existing user") {
        for
          userService <- ZIO.service[UserService]
          result <-
            userService.registerUser("test@test.com", "password", Some("Test"), Some("User")).exit
        yield assertTrue(result.isFailure)
      }.provide(userServiceLayer),
      test("validateCredentials should return user for valid credentials") {
        for
          userService <- ZIO.service[UserService]
          user <- userService.validateCredentials("test@test.com", "password")
        yield assertTrue(user.isDefined)
      }.provide(userServiceLayer),
      test("validateCredentials should fail for deactivated user") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.validateCredentials("testdeactivate@test.com", "password").exit
        yield assertTrue(result.isFailure)
      }.provide(userServiceLayer),
      test("updateUser should update user") {
        for
          userService <- ZIO.service[UserService]
          user <- userService.updateUser(UserId("1"), Some("New"), Some("User"))
        yield assertTrue(user.get.firstName == Some("New"))
      }.provide(userServiceLayer),
      test("updateUser should return None for deactivated user") {
        for
          userService <- ZIO.service[UserService]
          user <- userService.updateUser(UserId("3"), Some("New"), Some("User"))
        yield assertTrue(user.isEmpty)
      }.provide(userServiceLayer),
      test("updateUser should return None for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          user <-
            userService.updateUser(
              repo.asInstanceOf[MockUserRepository].nonExistentId,
              Some("New"),
              Some("User"),
            )
        yield assertTrue(user.isEmpty)
      }.provide(
        ZLayer.make[UserService & UserRepository](UserServiceImpl.layer, mockUserRepositoryLayer)
      ),
      test("changePassword should change password") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.changePassword(UserId("1"), "password", "newpassword")
        yield assertTrue(result)
      }.provide(userServiceLayer),
      test("changePassword should fail for deactivated user") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.changePassword(UserId("3"), "password", "newpassword").exit
        yield assertTrue(result.isFailure)
      }.provide(userServiceLayer),
      test("changePassword should fail for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          result <-
            userService
              .changePassword(
                repo.asInstanceOf[MockUserRepository].nonExistentId,
                "password",
                "newpassword",
              )
              .exit
        yield assertTrue(result.isFailure)
      }.provide(
        ZLayer.make[UserService & UserRepository](UserServiceImpl.layer, mockUserRepositoryLayer)
      ),
      test("deactivateUser should deactivate user") {
        for
          userService <- ZIO.service[UserService]
          result <- userService.deactivateUser(UserId("1"))
        yield assertTrue(result)
      }.provide(userServiceLayer),
      test("deactivateUser should return false for non-existent user") {
        for
          userService <- ZIO.service[UserService]
          repo <- ZIO.service[UserRepository]
          result <- userService.deactivateUser(repo.asInstanceOf[MockUserRepository].nonExistentId)
        yield assertTrue(!result)
      }.provide(
        ZLayer.make[UserService & UserRepository](UserServiceImpl.layer, mockUserRepositoryLayer)
      ),

      // Дополнительный тест: проверяем, что деактивированный пользователь не может войти
      test("validateCredentials should fail after user deactivation") {
        for
          userService <- ZIO.service[UserService]
          // Сначала деактивируем пользователя
          _ <- userService.deactivateUser(UserId("2"))
          // Затем пытаемся войти
          result <- userService.validateCredentials("test2@test.com", "password2").exit
        yield assertTrue(result.isFailure)
      }.provide(userServiceLayer),
    )
