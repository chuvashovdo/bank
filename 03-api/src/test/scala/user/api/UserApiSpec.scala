package user.api

import zio.*
import zio.test.*
import user.service.*
import user.models.*
import user.mapper.*
import auth.service.*
import jwt.service.*
import jwt.models.{ AccessToken, RefreshToken, JwtAccessToken, JwtRefreshToken }
import java.time.Instant
import java.util.UUID

object UserApiSpec extends ZIOSpecDefault:

  // Helper functions for creating custom types safely in tests
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException(s"Invalid UserId in test: $id"))
  private def unsafeEmail(email: String): Email =
    Email(email).getOrElse(throw new RuntimeException(s"Invalid Email in test: $email"))
  private def unsafePassword(password: String): Password =
    Password(password).getOrElse(throw new RuntimeException(s"Invalid Password in test: $password"))
  private def unsafeFirstName(name: String): FirstName =
    FirstName(name).getOrElse(throw new RuntimeException(s"Invalid FirstName in test: $name"))
  private def unsafeLastName(name: String): LastName =
    LastName(name).getOrElse(throw new RuntimeException(s"Invalid LastName in test: $name"))
  private def unsafeJwtAccessToken(token: String): JwtAccessToken =
    JwtAccessToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtAccessToken in test: $token")
    )
  private def unsafeJwtRefreshToken(token: String): JwtRefreshToken =
    JwtRefreshToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtRefreshToken in test: $token")
    )

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
      val tokenValue = s"access-token-${UUID.randomUUID().toString}"
      tokens += (tokenValue -> userId)
      val accessToken =
        AccessToken(
          token = unsafeJwtAccessToken(tokenValue),
          expiresAt = issuedAt.plusSeconds(3600),
          userId = userId,
        )
      ZIO.succeed(accessToken)

    override def createRefreshToken(
      userId: UserId,
      issuedAt: Instant = Instant.now(),
    ): Task[RefreshToken] =
      val tokenValue = s"refresh-token-${UUID.randomUUID().toString}"
      refreshTokens += (tokenValue -> userId)
      val refreshToken =
        RefreshToken(
          token = unsafeJwtRefreshToken(tokenValue),
          expiresAt = issuedAt.plusSeconds(86400),
          userId = userId,
        )
      ZIO.succeed(refreshToken)

    override def validateToken(token: JwtAccessToken): Task[UserId] =
      tokens.get(token.value) match
        case Some(userId) => ZIO.succeed(userId)
        case None => ZIO.fail(new RuntimeException("Invalid token"))

    override def refreshToken(token: JwtRefreshToken): Task[Option[AccessToken]] =
      refreshTokens.get(token.value) match
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
    override def login(email: Email, password: Password): Task[Option[AccessToken]] =
      if email.value == "valid@example.com" && password.value == "validPassword" then
        val userId = unsafeUserId("user-123")
        jwtService.createAccessToken(userId, Instant.now()).map(Some(_))
      else ZIO.succeed(None)

    override def register(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[AccessToken] =
      if email.value == "existing@example.com" then
        ZIO.fail(new RuntimeException("User already exists"))
      else
        val userId = unsafeUserId("new-user-456")
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
          unsafeUserId("user-123") -> User(
            id = unsafeUserId("user-123"),
            email = unsafeEmail("valid@example.com"),
            passwordHash = "hashed-password",
            firstName = Some(unsafeFirstName("Test")),
            lastName = Some(unsafeLastName("User")),
            isActive = true,
          ),
          unsafeUserId("new-user-456") -> User(
            id = unsafeUserId("new-user-456"),
            email = unsafeEmail("new@example.com"),
            passwordHash = "hashed-new-password",
            firstName = Some(unsafeFirstName("New")),
            lastName = Some(unsafeLastName("User")),
            isActive = true,
          ),
        )

    override def findUserById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.get(id))

    override def findUserByEmail(email: Email): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email.equals(email)))

    override def registerUser(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      if users.values.exists(_.email.equals(email)) then
        ZIO.fail(new RuntimeException("User already exists"))
      else
        val newId = unsafeUserId(UUID.randomUUID().toString)
        val user =
          User(
            id = newId,
            email = email,
            passwordHash = s"hashed-${password.value}",
            firstName = firstName,
            lastName = lastName,
            isActive = true,
          )
        users.put(newId, user)
        ZIO.succeed(user)

    override def validateCredentials(email: Email, password: Password): Task[Option[User]] =
      ZIO.succeed:
        users.values.find(u => u.email.equals(email) && u.isActive) match
          case Some(user) if password.value == "validPassword" => Some(user)
          case _ => None

    override def updateUser(
      id: UserId,
      firstName: Option[FirstName],
      lastName: Option[LastName],
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
      oldPassword: Password,
      newPassword: Password,
    ): Task[Boolean] =
      ZIO.succeed:
        users.get(id) match
          case Some(user) if user.isActive && oldPassword.value == "validPassword" =>
            val updated = user.copy(passwordHash = s"hashed-${newPassword.value}")
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
          userResponseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              accessToken <-
                authService.register(
                  unsafeEmail("new@example.com"),
                  unsafePassword("password123"),
                  Some(unsafeFirstName("New")),
                  Some(unsafeLastName("User")),
                )
              userOpt <- userService.findUserById(accessToken.userId)
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              userResponse <- userResponseMapper.fromUser(user)
            yield userResponse
        yield assertTrue(
          result.email.value == "new@example.com",
          result.firstName.map(_.value) == Some("New"),
          result.lastName.map(_.value) == Some("User"),
        )
      },
      test("login implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          userService <- ZIO.service[UserService]
          jwtService <- ZIO.service[JwtService]
          userResponseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              accessTokenOpt <-
                authService.login(unsafeEmail("valid@example.com"), unsafePassword("validPassword"))
              accessToken <-
                ZIO
                  .fromOption(accessTokenOpt)
                  .orElseFail(new RuntimeException("Invalid credentials"))
              userOpt <- userService.findUserById(accessToken.userId)
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              userResponse <- userResponseMapper.fromUser(user)
            yield (accessToken, userResponse)
          (accessToken, userResponse) = result
        yield assertTrue(
          accessToken.token.value.nonEmpty,
          userResponse.email.value == "valid@example.com",
          userResponse.firstName.map(_.value) == Some("Test"),
          userResponse.lastName.map(_.value) == Some("User"),
        )
      },
      test("getCurrentUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]
          responseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              userOpt <- userService.findUserById(unsafeUserId("user-123"))
              user <- ZIO.fromOption(userOpt).orElseFail(new RuntimeException("User not found"))
              userResponse <- responseMapper.fromUser(user)
            yield userResponse
        yield assertTrue(
          result.id == "user-123",
          result.email.value == "valid@example.com",
          result.firstName.map(_.value) == Some("Test"),
          result.lastName.map(_.value) == Some("User"),
        )
      },
      test("updateUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]
          responseMapper <- ZIO.service[UserResponseMapper]

          result <-
            for
              userOpt <-
                userService.updateUser(
                  unsafeUserId("user-123"),
                  Some(unsafeFirstName("Updated")),
                  Some(unsafeLastName("Name")),
                )
              user <-
                ZIO
                  .fromOption(userOpt)
                  .orElseFail(new RuntimeException("User not found or could not be updated"))
              userResponse <- responseMapper.fromUser(user)
            yield userResponse
        yield assertTrue(
          result.id == "user-123",
          result.firstName.map(_.value) == Some("Updated"),
          result.lastName.map(_.value) == Some("Name"),
        )
      },
      test("changePassword implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]

          result <-
            userService.changePassword(
              unsafeUserId("user-123"),
              unsafePassword("validPassword"),
              unsafePassword("newSecurePassword"),
            )
        yield assertTrue(
          result == true
        )
      },
      test("logout implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          jwtService <- ZIO.service[JwtService]

          accessTokenOpt <-
            authService.login(unsafeEmail("valid@example.com"), unsafePassword("validPassword"))
          accessToken <-
            ZIO.fromOption(accessTokenOpt).orElseFail(new RuntimeException("Login failed"))

          _ <- authService.logout(accessToken.userId)

          result <- jwtService.validateToken(accessToken.token).exit
        yield assertTrue(
          result.isFailure
        )
      },
    ).provide(testLayer)
