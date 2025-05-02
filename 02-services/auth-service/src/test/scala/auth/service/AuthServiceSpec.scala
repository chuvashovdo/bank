package auth.service

import zio.*
import zio.test.*
import user.service.UserService
import jwt.service.JwtService
import jwt.repository.TokenRepository
import user.models.*
import jwt.models.*
import java.time.Instant

object AuthServiceSpec extends ZIOSpecDefault:
  class MockUserService extends UserService:
    private var users: Map[String, User] =
      Map:
        "existing@example.com" -> User(
          UserId("existing-user"),
          "existing@example.com",
          // Хеш для пароля "password123"
          "$2a$12$vZUm.NSY.hOIiDvmX6YObetwMnzdLxX7R9vkwXKLgFQdLxQPyJZEC",
          Some("Existing"),
          Some("User"),
          true,
        )

    override def findUserById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.id.value == id.value))

    override def findUserByEmail(email: String): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email == email))

    override def registerUser(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[User] =
      // Эмулируем ошибку, если пользователь уже существует
      if users.contains(email) then ZIO.fail(new RuntimeException("User already exists"))
      else
        val user =
          User(
            UserId(s"user-${users.size + 1}"),
            email,
            s"hashed-$password", // Упрощённый хеш для тестов
            firstName,
            lastName,
            true,
          )
        users = users + (email -> user)
        ZIO.succeed(user)

    override def validateCredentials(email: String, password: String): Task[Option[User]] =
      for
        userOpt <- findUserByEmail(email)
        // Простая проверка пароля для тестов
        result = userOpt.filter(_ => password == "password123")
      yield result

    override def updateUser(
      id: UserId,
      firstName: Option[String],
      lastName: Option[String],
    ): Task[Option[User]] =
      ZIO.succeed(None)

    override def changePassword(
      id: UserId,
      oldPassword: String,
      newPassword: String,
    ): Task[Boolean] =
      ZIO.succeed(false)

    override def deactivateUser(id: UserId): Task[Boolean] =
      ZIO.succeed(false)

  class MockJwtService extends JwtService:
    override def createAccessToken(userId: UserId, issuedAt: Instant): Task[AccessToken] =
      ZIO.succeed(
        AccessToken(
          s"access-token-for-${userId.value}",
          issuedAt.plusSeconds(3600),
          userId,
        )
      )

    override def createRefreshToken(userId: UserId, issuedAt: Instant): Task[RefreshToken] =
      ZIO.succeed(
        RefreshToken(
          s"refresh-token-for-${userId.value}",
          issuedAt.plusSeconds(86400),
          userId,
        )
      )

    override def validateToken(token: String): Task[UserId] =
      if token.startsWith("valid-token") then ZIO.succeed(UserId("existing-user"))
      else if token.startsWith("invalid-token") then
        ZIO.fail(new IllegalArgumentException("Invalid token"))
      else if token.startsWith("expired-token") then
        ZIO.fail(new IllegalArgumentException("Token expired"))
      else ZIO.fail(new IllegalArgumentException("Unknown token"))

  class MockTokenRepository extends TokenRepository:
    // Моковое хранилище токенов
    private var tokens: Map[String, RefreshToken] =
      Map.empty

    override def saveRefreshToken(token: RefreshToken): Task[Unit] =
      ZIO.succeed:
        tokens = tokens + (token.token -> token)

    override def findByRefreshToken(refreshToken: String): Task[Option[RefreshToken]] =
      ZIO.succeed(tokens.get(refreshToken))

    override def deleteByRefreshToken(refreshToken: String): Task[Unit] =
      ZIO.succeed:
        tokens = tokens - refreshToken

    override def deleteAllByUserId(userId: UserId): Task[Unit] =
      ZIO.succeed:
        tokens = tokens.filter { case (_, token) => token.userId.value != userId.value }
    override def cleanExpiredTokens(): Task[Unit] =
      ZIO.succeed(())

  // Создаём тестовые слои
  val mockUserServiceLayer =
    ZLayer.succeed(new MockUserService)
  val mockJwtServiceLayer =
    ZLayer.succeed(new MockJwtService)
  val mockTokenRepositoryLayer =
    ZLayer.succeed(new MockTokenRepository)

  // Комбинируем слои для создания тестовой реализации AuthService
  val testAuthServiceLayer =
    ZLayer.make[AuthService](
      mockUserServiceLayer,
      mockJwtServiceLayer,
      mockTokenRepositoryLayer,
      AuthServiceImpl.layer,
    )

  // Сами тесты
  def spec =
    suite("AuthService")(
      test("login should return token for valid credentials") {
        for
          authService <- ZIO.service[AuthService]
          tokenOpt <- authService.login("existing@example.com", "password123")
        yield assertTrue(
          tokenOpt.isDefined,
          tokenOpt.map(_.userId.value).contains("existing-user"),
        )
      }.provide(testAuthServiceLayer),
      test("login should return None for invalid credentials") {
        for
          authService <- ZIO.service[AuthService]
          tokenOpt <- authService.login("existing@example.com", "wrong-password")
        yield assertTrue(tokenOpt.isEmpty)
      }.provide(testAuthServiceLayer),
      test("register should create user and return token") {
        for
          authService <- ZIO.service[AuthService]
          token <-
            authService.register(
              "new@example.com",
              "secure-password",
              Some("New"),
              Some("User"),
            )
        yield assertTrue(
          token.token.contains("access-token"),
          token.userId.value.nonEmpty,
        )
      }.provide(testAuthServiceLayer),
      test("register should fail for existing user") {
        for
          authService <- ZIO.service[AuthService]
          result <-
            authService
              .register(
                "existing@example.com",
                "password",
                None,
                None,
              )
              .exit
        yield assertTrue(result.isFailure)
      }.provide(testAuthServiceLayer),
      test("validateToken should return userId for valid token") {
        for
          authService <- ZIO.service[AuthService]
          userIdOpt <- authService.validateToken("valid-token-123")
        yield assertTrue(
          userIdOpt.isDefined,
          userIdOpt.map(_.value).contains("existing-user"),
        )
      }.provide(testAuthServiceLayer),
      test("validateToken should return None for invalid token") {
        for
          authService <- ZIO.service[AuthService]
          userIdOpt <- authService.validateToken("invalid-token-123")
        yield assertTrue(userIdOpt.isEmpty)
      }.provide(testAuthServiceLayer),
      test("refreshToken should create new access token and invalidate refresh token") {
        for
          authService <- ZIO.service[AuthService]
          tokenRepo <- ZIO.service[TokenRepository]

          userId = UserId("test-user")
          refreshToken = RefreshToken("test-refresh-token", Instant.now().plusSeconds(3600), userId)
          _ <- tokenRepo.saveRefreshToken(refreshToken)

          newTokenOpt <- authService.refreshToken("test-refresh-token")

          oldToken <- tokenRepo.findByRefreshToken("test-refresh-token")
        yield assertTrue(
          newTokenOpt.isDefined,
          newTokenOpt.map(_.userId).contains(userId),
          oldToken.isEmpty,
        )
      }.provide(
        testAuthServiceLayer,
        mockTokenRepositoryLayer,
      ),
      test("logout should remove all refresh tokens for user") {
        for

          authService <- ZIO.service[AuthService]
          tokenRepo <- ZIO.service[TokenRepository]

          userId = UserId("logout-test-user")
          refreshToken1 = RefreshToken("refresh-token-1", Instant.now().plusSeconds(3600), userId)
          refreshToken2 = RefreshToken("refresh-token-2", Instant.now().plusSeconds(3600), userId)
          _ <- tokenRepo.saveRefreshToken(refreshToken1)
          _ <- tokenRepo.saveRefreshToken(refreshToken2)

          _ <- authService.logout(userId)

          token1 <- tokenRepo.findByRefreshToken("refresh-token-1")
          token2 <- tokenRepo.findByRefreshToken("refresh-token-2")
        yield assertTrue(
          token1.isEmpty,
          token2.isEmpty,
        )
      }.provide(
        testAuthServiceLayer,
        mockTokenRepositoryLayer,
      ),
    )
