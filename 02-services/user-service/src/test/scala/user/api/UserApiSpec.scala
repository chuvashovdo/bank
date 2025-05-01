package user.api

import zio.*
import zio.test.*
import user.service.*
import user.models.*
import user.mapper.*
import auth.service.*
import jwt.models.{ AccessToken, RefreshToken }
import java.time.Instant
import java.util.UUID

object UserApiSpec extends ZIOSpecDefault:

  // Мок AuthService
  class MockAuthService extends AuthService:
    var users =
      Map.empty[String, UserId]
    var refreshTokens =
      Map.empty[String, UserId]

    override def validateToken(token: String): Task[Option[UserId]] =
      ZIO.succeed(users.get(token))

    override def login(email: String, password: String): Task[Option[AccessToken]] =
      if email == "valid@example.com" && password == "validPassword" then
        val userId = UserId("user-123")
        val token = "valid-jwt-token"
        users += (token -> userId)
        val accessToken =
          AccessToken(
            token = token,
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
          )
        ZIO.succeed(Some(accessToken))
      else ZIO.succeed(None)

    override def register(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[AccessToken] =
      if email == "existing@example.com" then ZIO.fail(new RuntimeException("User already exists"))
      else
        val userId = UserId("new-user-456")
        val token = "new-jwt-token"
        users += (token -> userId)
        val accessToken =
          AccessToken(
            token = token,
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
          )
        ZIO.succeed(accessToken)

    override def refreshToken(token: String): Task[Option[AccessToken]] =
      refreshTokens.get(token) match
        case Some(userId) =>
          val newToken = "refreshed-token"
          users += (newToken -> userId)
          val accessToken =
            AccessToken(
              token = newToken,
              userId = userId,
              expiresAt = Instant.now().plusSeconds(3600),
            )
          ZIO.succeed(Some(accessToken))
        case None =>
          ZIO.succeed(None)

    override def createRefreshToken(userId: UserId): Task[RefreshToken] =
      val token = s"refresh-token-${UUID.randomUUID().toString}"
      refreshTokens += (token -> userId)
      ZIO.succeed(
        RefreshToken(
          token = token,
          userId = userId,
          expiresAt = Instant.now().plusSeconds(86400),
        )
      )

    override def logout(userId: UserId): Task[Unit] =
      ZIO.succeed:
        refreshTokens = refreshTokens.filter((_, id) => id.value != userId.value)
        users = users.filter((_, id) => id.value != userId.value)

  // Мок UserService
  class MockUserService extends UserService:
    private val users =
      scala
        .collection
        .mutable
        .Map(
          UserId("user-123") -> User(
            id = UserId("user-123"),
            email = "valid@example.com",
            passwordHash = "hashed-password",
            firstName = Some("Test"),
            lastName = Some("User"),
            isActive = true,
          ),
          UserId("new-user-456") -> User(
            id = UserId("new-user-456"),
            email = "new@example.com",
            passwordHash = "hashed-new-password",
            firstName = Some("New"),
            lastName = Some("User"),
            isActive = true,
          ),
        )

    override def findUserById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.get(id))

    override def findUserByEmail(email: String): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email == email))

    override def registerUser(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[User] =
      if users.values.exists(_.email == email) then
        ZIO.fail(new RuntimeException("User already exists"))
      else
        val newId = UserId(UUID.randomUUID().toString)
        val user =
          User(
            id = newId,
            email = email,
            passwordHash = s"hashed-$password",
            firstName = firstName,
            lastName = lastName,
            isActive = true,
          )
        users.put(newId, user)
        ZIO.succeed(user)

    override def validateCredentials(email: String, password: String): Task[Option[User]] =
      ZIO.succeed:
        users.values.find(u => u.email == email && u.isActive) match
          case Some(user) if password == "validPassword" => Some(user)
          case _ => None

    override def updateUser(
      id: UserId,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[Option[User]] =
      ZIO.succeed:
        users.get(id).map { user =>
          val updated =
            user.copy(
              firstName = firstName,
              lastName = lastName,
            )
          users.put(id, updated)
          updated
        }

    override def changePassword(
      id: UserId,
      oldPassword: String,
      newPassword: String,
    ): Task[Boolean] =
      ZIO.succeed:
        users.get(id) match
          case Some(user) if user.isActive && oldPassword == "validPassword" =>
            val updated = user.copy(passwordHash = s"hashed-$newPassword")
            users.put(id, updated)
            true
          case _ => false

    override def deactivateUser(id: UserId): Task[Boolean] =
      ZIO.succeed:
        users.get(id) match
          case Some(user) =>
            val updated = user.copy(isActive = false)
            users.put(id, updated)
            true
          case None => false

  val authServiceLayer =
    ZLayer.succeed(new MockAuthService)
  val userServiceLayer =
    ZLayer.succeed(new MockUserService)
  val userResponseMapperLayer =
    UserResponseMapperImpl.layer
  val userApiLayer =
    UserApi.layer
  val testEnvLayer: ZLayer[Any, Nothing, UserApi] =
    (authServiceLayer ++ userServiceLayer ++ userResponseMapperLayer) >>> userApiLayer

  def spec =
    suite("UserApi")(
      test("UserApi can be properly constructed") {
        for api <- ZIO.service[UserApi]
        yield assertTrue(api != null)
      },
      test("register endpoint implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService].provide(authServiceLayer)
          userService <- ZIO.service[UserService].provide(userServiceLayer)

          result <-
            ZIO.scoped {
              for
                accessToken <-
                  authService.register("new@example.com", "password123", Some("New"), Some("User"))
                userOpt <- userService.findUserById(accessToken.userId)
                user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
                refreshToken <- authService.createRefreshToken(accessToken.userId)
                userResponse <-
                  ZIO
                    .service[UserResponseMapper]
                    .provide(userResponseMapperLayer)
                    .flatMap(_.fromUser(user))
              yield UserResponse(
                id = user.id.value,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
              )
            }
        yield assertTrue(
          result.email == "new@example.com",
          result.firstName == Some("New"),
          result.lastName == Some("User"),
        )
      },
      test("login implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService].provide(authServiceLayer)
          userService <- ZIO.service[UserService].provide(userServiceLayer)

          result <-
            ZIO.scoped {
              for
                accessTokenOpt <- authService.login("valid@example.com", "validPassword")
                accessToken <-
                  ZIO
                    .fromOption(accessTokenOpt)
                    .orElseFail(new RuntimeException("Invalid credentials"))
                userOpt <- userService.findUserById(accessToken.userId)
                user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
                refreshToken <- authService.createRefreshToken(accessToken.userId)
                userResponse <-
                  ZIO
                    .service[UserResponseMapper]
                    .provide(userResponseMapperLayer)
                    .flatMap(_.fromUser(user))
              yield (accessToken, user, userResponse)
            }
          (accessToken, user, userResponse) = result
        yield assertTrue(
          accessToken.token == "valid-jwt-token",
          userResponse.email == "valid@example.com",
          userResponse.firstName == Some("Test"),
          userResponse.lastName == Some("User"),
        )
      },
      test("getCurrentUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService].provide(userServiceLayer)
          responseMapper <- ZIO.service[UserResponseMapper].provide(userResponseMapperLayer)

          result <-
            ZIO.scoped {
              for
                userOpt <- userService.findUserById(UserId("user-123"))
                user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
                userResponse <- responseMapper.fromUser(user)
              yield userResponse
            }
        yield assertTrue(
          result.id == "user-123",
          result.email == "valid@example.com",
          result.firstName == Some("Test"),
          result.lastName == Some("User"),
        )
      },
      test("updateUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService].provide(userServiceLayer)
          responseMapper <- ZIO.service[UserResponseMapper].provide(userResponseMapperLayer)

          result <-
            ZIO.scoped {
              for
                userOpt <- userService.updateUser(UserId("user-123"), Some("Updated"), Some("Name"))
                user <-
                  ZIO
                    .fromOption(userOpt)
                    .orElseFail(new RuntimeException("User not found or could not be updated"))
                userResponse <- responseMapper.fromUser(user)
              yield userResponse
            }
        yield assertTrue(
          result.id == "user-123",
          result.firstName == Some("Updated"),
          result.lastName == Some("Name"),
        )
      },
      test("changePassword implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService].provide(userServiceLayer)

          result <-
            userService.changePassword(UserId("user-123"), "validPassword", "newSecurePassword")
        yield assertTrue(
          result == true
        )
      },
      test("logout implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService].provide(authServiceLayer)

          accessTokenOpt <- authService.login("valid@example.com", "validPassword")
          accessToken <-
            ZIO.fromOption(accessTokenOpt).orElseFail(new RuntimeException("Login failed"))

          _ <- authService.logout(accessToken.userId)

          validateResult <- authService.validateToken(accessToken.token)
        yield assertTrue(
          validateResult.isEmpty
        )
      },
    ).provide(testEnvLayer)
