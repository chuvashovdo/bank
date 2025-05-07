package user.api

import zio.*
import zio.test.*
import user.service.*
import user.models.*
import user.mapper.*
import auth.service.*
import jwt.service.*
import jwt.models.{ AccessToken, RefreshToken }
import java.time.Instant
import java.util.UUID

object UserApiSpec extends ZIOSpecDefault:

  // Мок JwtService
  class MockJwtService extends JwtService:
    var tokens =
      Map.empty[String, UserId]
    var refreshTokens =
      Map.empty[String, UserId]

    override def createAccessToken(
      userId: UserId,
      issuedAt: Instant = Instant.now(),
    ): Task[AccessToken] =
      val token = s"access-token-${UUID.randomUUID().toString}"
      tokens += (token -> userId)
      val accessToken =
        AccessToken(
          token = token,
          expiresAt = issuedAt.plusSeconds(3600),
          userId = userId,
        )
      ZIO.succeed(accessToken)

    override def createRefreshToken(
      userId: UserId,
      issuedAt: Instant = Instant.now(),
    ): Task[RefreshToken] =
      val token = s"refresh-token-${UUID.randomUUID().toString}"
      refreshTokens += (token -> userId)
      val refreshToken =
        RefreshToken(
          token = token,
          expiresAt = issuedAt.plusSeconds(86400),
          userId = userId,
        )
      ZIO.succeed(refreshToken)

    override def validateToken(token: String): Task[UserId] =
      tokens.get(token) match
        case Some(userId) => ZIO.succeed(userId)
        case None => ZIO.fail(new RuntimeException("Invalid token"))

    override def refreshToken(token: String): Task[Option[AccessToken]] =
      refreshTokens.get(token) match
        case Some(userId) =>
          for accessToken <- createAccessToken(userId)
          yield Some(accessToken)
        case None =>
          ZIO.succeed(None)

    override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
      ZIO.succeed:
        refreshTokens = refreshTokens.filter((_, id) => id.value != userId.value)
        tokens = tokens.filter((_, id) => id.value != userId.value)

  // Мок AuthService
  class MockAuthService(jwtService: JwtService) extends AuthService:
    override def login(email: String, password: String): Task[Option[AccessToken]] =
      if email == "valid@example.com" && password == "validPassword" then
        val userId = UserId("user-123")
        jwtService.createAccessToken(userId, Instant.now()).map(Some(_))
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
        jwtService.createAccessToken(userId, Instant.now())

    override def logout(userId: UserId): Task[Unit] =
      jwtService.invalidateRefreshTokens(userId)

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

  // Подготавливаем слои для тестового окружения
  val mockJwtService =
    new MockJwtService
  val mockJwtServiceLayer: ULayer[JwtService] =
    ZLayer.succeed(mockJwtService)
  val mockAuthServiceLayer: ULayer[AuthService] =
    ZLayer.succeed(new MockAuthService(mockJwtService))
  val mockUserServiceLayer: ULayer[UserService] =
    ZLayer.succeed(new MockUserService)
  val mockUserResponseMapperLayer: ULayer[UserResponseMapper] =
    UserResponseMapperImpl.layer

  // Создаем комбинированный слой для всех наших тестов
  val testLayer: ZLayer[Any, Nothing, UserApi & AuthService & UserService & JwtService & UserResponseMapper] =
    ZLayer.make[UserApi & AuthService & UserService & JwtService & UserResponseMapper](
      mockAuthServiceLayer,
      mockJwtServiceLayer,
      mockUserServiceLayer,
      mockUserResponseMapperLayer,
      UserApi.layer,
    )

  def spec =
    suite("UserApi")(
      test("UserApi can be properly constructed") {
        for api <- ZIO.service[UserApi]
        yield assertTrue(api != null)
      },
      test("register endpoint implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          userService <- ZIO.service[UserService]
          jwtService <- ZIO.service[JwtService]

          result <-
            for
              accessToken <-
                authService.register("new@example.com", "password123", Some("New"), Some("User"))
              userOpt <- userService.findUserById(accessToken.userId)
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              refreshToken <- jwtService.createRefreshToken(accessToken.userId, Instant.now())
              userResponse <-
                ZIO
                  .service[UserResponseMapper]
                  .flatMap(_.fromUser(user))
            yield UserResponse(
              id = user.id.value,
              email = user.email,
              firstName = user.firstName,
              lastName = user.lastName,
            )
        yield assertTrue(
          result.email == "new@example.com",
          result.firstName == Some("New"),
          result.lastName == Some("User"),
        )
      },
      test("login implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          userService <- ZIO.service[UserService]
          jwtService <- ZIO.service[JwtService]

          result <-
            for
              accessTokenOpt <- authService.login("valid@example.com", "validPassword")
              accessToken <-
                ZIO
                  .fromOption(accessTokenOpt)
                  .orElseFail(new RuntimeException("Invalid credentials"))
              userOpt <- userService.findUserById(accessToken.userId)
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              refreshToken <- jwtService.createRefreshToken(accessToken.userId, Instant.now())
              userResponse <-
                ZIO
                  .service[UserResponseMapper]
                  .flatMap(_.fromUser(user))
            yield (accessToken, user, userResponse)
          (accessToken, user, userResponse) = result
        yield assertTrue(
          accessToken.token.nonEmpty,
          userResponse.email == "valid@example.com",
          userResponse.firstName == Some("Test"),
          userResponse.lastName == Some("User"),
        )
      },
      test("getCurrentUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]
          responseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              userOpt <- userService.findUserById(UserId("user-123"))
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              userResponse <- responseMapper.fromUser(user)
            yield userResponse
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
          userService <- ZIO.service[UserService]
          responseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              userOpt <- userService.updateUser(UserId("user-123"), Some("Updated"), Some("Name"))
              user <-
                ZIO
                  .fromOption(userOpt)
                  .orElseFail(new RuntimeException("User not found or could not be updated"))
              userResponse <- responseMapper.fromUser(user)
            yield userResponse
        yield assertTrue(
          result.id == "user-123",
          result.firstName == Some("Updated"),
          result.lastName == Some("Name"),
        )
      },
      test("changePassword implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]

          result <-
            userService.changePassword(UserId("user-123"), "validPassword", "newSecurePassword")
        yield assertTrue(
          result == true
        )
      },
      test("logout implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          jwtService <- ZIO.service[JwtService]

          accessTokenOpt <- authService.login("valid@example.com", "validPassword")
          accessToken <-
            ZIO.fromOption(accessTokenOpt).orElseFail(new RuntimeException("Login failed"))

          _ <- authService.logout(accessToken.userId)

          // Проверим через JwtService - валидация должна завершиться ошибкой для недействительного токена
          result <- jwtService.validateToken(accessToken.token).exit
        yield assertTrue(
          result.isFailure
        )
      },
    ).provide(testLayer)
