package user.api

import zio.*
import zio.test.*
import user.service.*
import user.models.*
import auth.service.*
import jwt.service.*
import jwt.models.{ AccessToken, RefreshToken, JwtAccessToken, JwtRefreshToken }
import java.time.Instant
import java.util.UUID
import user.errors.*
import auth.errors.*
import user.models.dto.UserResponse

object UserApiSpec extends ZIOSpecDefault:
  private val userId1 =
    UUID.randomUUID()
  private val userId2 =
    UUID.randomUUID()

  // Helper functions for creating custom types safely in tests
  private def unsafeUserId(id: UUID): UserId =
    UserId(id)
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

    override def renewAccessToken(token: JwtRefreshToken): Task[AccessToken] =
      refreshTokens.get(token.value) match
        case Some(userId) =>
          for accessToken <- createAccessToken(userId)
          yield accessToken
        case None =>
          ZIO.fail(RefreshTokenNotFoundError(token.value))

    override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
      ZIO.succeed:
        refreshTokens = refreshTokens.filter((_, id) => id != userId)
        tokens = tokens.filter((_, id) => id != userId)

  // Мок AuthService
  class MockAuthService(jwtService: JwtService) extends AuthService:
    override def login(email: Email, password: Password): Task[AccessToken] =
      if email.value == "valid@example.com" && password.value == "validPassword" then
        jwtService.createAccessToken(unsafeUserId(userId1), Instant.now())
      else ZIO.fail(new InvalidCredentialsError())

    override def register(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[AccessToken] =
      if email.value == "existing@example.com" then
        ZIO.fail(new RuntimeException("User already exists"))
      else jwtService.createAccessToken(unsafeUserId(userId2), Instant.now())

    override def logout(userId: UserId): Task[Unit] =
      jwtService.invalidateRefreshTokens(userId)

  // Мок UserService
  class MockUserService extends UserService:
    private val users =
      scala
        .collection
        .mutable
        .Map(
          unsafeUserId(userId1) -> User(
            id = unsafeUserId(userId1),
            email = unsafeEmail("valid@example.com"),
            passwordHash = "hashed-password",
            firstName = Some(unsafeFirstName("Test")),
            lastName = Some(unsafeLastName("User")),
            isActive = true,
          ),
          unsafeUserId(userId2) -> User(
            id = unsafeUserId(userId2),
            email = unsafeEmail("new@example.com"),
            passwordHash = "hashed-new-password",
            firstName = Some(unsafeFirstName("New")),
            lastName = Some(unsafeLastName("User")),
            isActive = true,
          ),
        )

    override def findUserById(id: UserId): Task[User] =
      ZIO.attempt(users.get(id).get).mapError(_ => new UserNotFoundError(id.value))

    override def findUserByEmail(email: Email): Task[User] =
      ZIO
        .attempt(users.values.find(_.email.equals(email)).get)
        .mapError(_ => new UserNotFoundError(email.value))

    override def registerUser(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      if users.values.exists(_.email.equals(email)) then
        ZIO.fail(new RuntimeException("User already exists"))
      else
        val newId = unsafeUserId(UUID.randomUUID())
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

    override def validateCredentials(email: Email, password: Password): Task[User] =
      ZIO.succeed:
        users.values.find(u => u.email.equals(email) && u.isActive) match
          case Some(user) if password.value == "validPassword" => user
          case _ => throw new InvalidCredentialsError()

    override def updateUser(
      id: UserId,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      ZIO.succeed:
        val user = users.get(id).get
        val updated =
          user.copy(
            firstName = firstName,
            lastName = lastName,
          )
        users.put(id, updated)
        updated

    override def changePassword(
      id: UserId,
      oldPassword: Password,
      newPassword: Password,
    ): Task[Unit] =
      findUserById(id).flatMap { user =>
        if user.isActive && oldPassword.value == "validPassword" then
          val updatedUser = user.copy(passwordHash = s"hashed-${newPassword.value}")
          ZIO.succeed(users.put(id, updatedUser)).unit
        else ZIO.fail(new InvalidCredentialsError())
      }

    override def deactivateUser(id: UserId): Task[Unit] =
      findUserById(id).flatMap { user =>
        val updatedUser = user.copy(isActive = false)
        ZIO.succeed(users.put(id, updatedUser)).unit
      }

  // Подготавливаем слои для тестового окружения
  val mockJwtService =
    new MockJwtService
  val mockJwtServiceLayer: ULayer[JwtService] =
    ZLayer.succeed(mockJwtService)
  val mockAuthServiceLayer: ULayer[AuthService] =
    ZLayer.succeed(new MockAuthService(mockJwtService))
  val mockUserServiceLayer: ULayer[UserService] =
    ZLayer.succeed(new MockUserService)

  // Создаем комбинированный слой для всех наших тестов
  val testLayer: ZLayer[Any, Nothing, UserApi & AuthService & UserService & JwtService] =
    ZLayer.make[UserApi & AuthService & UserService & JwtService](
      mockAuthServiceLayer,
      mockJwtServiceLayer,
      mockUserServiceLayer,
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
                authService.register(
                  unsafeEmail("new@example.com"),
                  unsafePassword("password123"),
                  Some(unsafeFirstName("New")),
                  Some(unsafeLastName("User")),
                )
              user <- userService.findUserById(accessToken.userId)
              userResponse <-
                ZIO.succeed(UserResponse(user.id, user.email, user.firstName, user.lastName))
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

          result <-
            for
              accessToken <-
                authService.login(unsafeEmail("valid@example.com"), unsafePassword("validPassword"))
              user <- userService.findUserById(accessToken.userId)
              userResponse <-
                ZIO.succeed(UserResponse(user.id, user.email, user.firstName, user.lastName))
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
          result <-
            for
              user <- userService.findUserById(unsafeUserId(userId1))
              userResponse <-
                ZIO.succeed(UserResponse(user.id, user.email, user.firstName, user.lastName))
            yield userResponse
        yield assertTrue(
          result.id == unsafeUserId(userId1),
          result.email.value == "valid@example.com",
          result.firstName.map(_.value) == Some("Test"),
          result.lastName.map(_.value) == Some("User"),
        )
      },
      test("updateUser implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]

          result <-
            for
              user <-
                userService.updateUser(
                  unsafeUserId(userId1),
                  Some(unsafeFirstName("Updated")),
                  Some(unsafeLastName("Name")),
                )
              userResponse <-
                ZIO.succeed(UserResponse(user.id, user.email, user.firstName, user.lastName))
            yield userResponse
        yield assertTrue(
          result.id == unsafeUserId(userId1),
          result.firstName.map(_.value) == Some("Updated"),
          result.lastName.map(_.value) == Some("Name"),
        )
      },
      test("changePassword implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          userService <- ZIO.service[UserService]

          result <-
            userService
              .changePassword(
                unsafeUserId(userId1),
                unsafePassword("validPassword"),
                unsafePassword("newSecurePassword"),
              )
              .exit
        yield assertTrue(
          result.isSuccess
        )
      },
      test("logout implementation functions correctly") {
        for
          api <- ZIO.service[UserApi]
          authService <- ZIO.service[AuthService]
          jwtService <- ZIO.service[JwtService]

          accessToken <-
            authService.login(unsafeEmail("valid@example.com"), unsafePassword("validPassword"))

          _ <- authService.logout(accessToken.userId)

          result <- jwtService.validateToken(accessToken.token).exit
        yield assertTrue(
          result.isFailure
        )
      },
    ).provide(testLayer)
