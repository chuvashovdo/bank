package auth.service

import zio.*
import zio.test.*
import user.service.UserService
import jwt.service.JwtService
import jwt.repository.TokenRepository
import user.models.*
import user.models.Password
import jwt.models.*
import jwt.models.{ JwtAccessToken, JwtRefreshToken }
import java.time.Instant

object AuthServiceSpec extends ZIOSpecDefault:
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException(s"Invalid UserId in test setup: $id"))
  private def unsafeEmail(email: String): Email =
    Email(email).getOrElse(throw new RuntimeException(s"Invalid Email in test setup: $email"))
  private def unsafePassword(password: String): Password =
    Password(password).getOrElse(
      throw new RuntimeException(s"Invalid Password in test setup: $password")
    )
  private def unsafeFirstName(name: String): FirstName =
    FirstName(name).getOrElse(throw new RuntimeException(s"Invalid FirstName in test setup: $name"))
  private def unsafeLastName(name: String): LastName =
    LastName(name).getOrElse(throw new RuntimeException(s"Invalid LastName in test setup: $name"))
  private def unsafeJwtAccessToken(token: String): JwtAccessToken =
    JwtAccessToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtAccessToken in test setup: $token")
    )
  private def unsafeJwtRefreshToken(token: String): JwtRefreshToken =
    JwtRefreshToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtRefreshToken in test setup: $token")
    )

  class MockUserService extends UserService:
    private def internalUnsafeUserId(id: String): UserId =
      UserId(id).getOrElse(
        throw new RuntimeException(s"Invalid UserId in MockUserService setup: $id")
      )
    private def internalUnsafeEmail(email: String): Email =
      Email(email).getOrElse(
        throw new RuntimeException(s"Invalid Email in MockUserService setup: $email")
      )
    private def internalUnsafeFirstName(name: String): FirstName =
      FirstName(name).getOrElse(
        throw new RuntimeException(s"Invalid FirstName in MockUserService setup: $name")
      )
    private def internalUnsafeLastName(name: String): LastName =
      LastName(name).getOrElse(
        throw new RuntimeException(s"Invalid LastName in MockUserService setup: $name")
      )

    private var users: Map[String, User] =
      Map:
        "existing@example.com" -> User(
          internalUnsafeUserId("existing-user"),
          internalUnsafeEmail("existing@example.com"),
          "$2a$12$vZUm.NSY.hOIiDvmX6YObetwMnzdLxX7R9vkwXKLgFQdLxQPyJZEC",
          Some(internalUnsafeFirstName("Existing")),
          Some(internalUnsafeLastName("User")),
          true,
        )

    override def findUserById(id: UserId): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.id.equals(id)))

    override def findUserByEmail(email: Email): Task[Option[User]] =
      ZIO.succeed(users.values.find(_.email.equals(email)))

    override def registerUser(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      if users.contains(email.value) then ZIO.fail(new RuntimeException("User already exists"))
      else
        val user =
          User(
            internalUnsafeUserId(s"user-${users.size + 1}"),
            email,
            s"hashed-${password.value}",
            firstName,
            lastName,
            true,
          )
        users = users + (email.value -> user)
        ZIO.succeed(user)

    override def validateCredentials(email: Email, password: Password): Task[Option[User]] =
      for
        userOpt <- findUserByEmail(email)
        result = userOpt.filter(_ => password.value == "password123")
      yield result

    override def updateUser(
      id: UserId,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[Option[User]] =
      ZIO.succeed(None)

    override def changePassword(
      id: UserId,
      oldPassword: Password,
      newPassword: Password,
    ): Task[Boolean] =
      ZIO.succeed(false)

    override def deactivateUser(id: UserId): Task[Boolean] =
      ZIO.succeed(false)

  class MockJwtService(tokenRepository: TokenRepository) extends JwtService:
    private var validTokens: Map[JwtAccessToken, UserId] =
      Map.empty

    override def createAccessToken(userId: UserId, issuedAt: Instant): Task[AccessToken] =
      val tokenValue = s"access-token-for-${userId.value}"
      val jwtToken = unsafeJwtAccessToken(tokenValue)
      validTokens = validTokens + (jwtToken -> userId)
      ZIO.succeed(
        AccessToken(
          token = jwtToken,
          expiresAt = issuedAt.plusSeconds(3600),
          userId = userId,
        )
      )

    override def createRefreshToken(userId: UserId, issuedAt: Instant): Task[RefreshToken] =
      val tokenValue = s"refresh-token-for-${userId.value}"
      val jwtRefreshToken = unsafeJwtRefreshToken(tokenValue)
      val refreshToken =
        RefreshToken(
          jwtRefreshToken,
          issuedAt.plusSeconds(86400),
          userId,
        )
      tokenRepository.saveRefreshToken(refreshToken).as(refreshToken)

    override def validateToken(token: JwtAccessToken): Task[UserId] =
      validTokens.find { case (jwtAccessToken, _) => jwtAccessToken.value == token.value } match
        case Some((_, userId)) => ZIO.succeed(userId)
        case None =>
          if token.value.startsWith("valid-user-id-from-string-token") then
            ZIO.succeed(unsafeUserId("existing-user"))
          else if token.value.startsWith("invalid-token") then
            ZIO.fail(new IllegalArgumentException("Invalid token"))
          else if token.value.startsWith("expired-token") then
            ZIO.fail(new IllegalArgumentException("Token expired"))
          else ZIO.fail(new IllegalArgumentException("Unknown token"))

    override def refreshToken(token: JwtRefreshToken): Task[Option[AccessToken]] =
      for
        refreshTokenOpt <- tokenRepository.findByRefreshToken(token)
        result <-
          refreshTokenOpt match
            case Some(foundRefreshToken) =>
              for
                _ <- tokenRepository.deleteByRefreshToken(token)
                accessToken <- createAccessToken(foundRefreshToken.userId, Instant.now())
              yield Some(accessToken)
            case None =>
              ZIO.succeed(None)
      yield result

    override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
      tokenRepository.deleteAllByUserId(userId)

  class MockTokenRepository extends TokenRepository:
    private var tokens: Map[JwtRefreshToken, RefreshToken] =
      Map.empty

    override def saveRefreshToken(token: RefreshToken): Task[Unit] =
      ZIO.succeed:
        tokens = tokens + (token.token -> token)

    override def findByRefreshToken(refreshToken: JwtRefreshToken): Task[Option[RefreshToken]] =
      ZIO.succeed(tokens.get(refreshToken))

    override def deleteByRefreshToken(refreshToken: JwtRefreshToken): Task[Unit] =
      for
        _ <- ZIO.debug(s"Deleting token: $refreshToken, current tokens: $tokens")
        _ <- ZIO.succeed { tokens = tokens - refreshToken }
        _ <- ZIO.debug(s"Tokens after deletion: $tokens")
      yield ()

    override def deleteAllByUserId(userId: UserId): Task[Unit] =
      ZIO.succeed:
        tokens = tokens.filter { case (_, token) => token.userId.value != userId.value }
    override def cleanExpiredTokens(): Task[Unit] =
      ZIO.succeed(())

  val mockUserServiceLayer: ULayer[UserService] =
    ZLayer.succeed(new MockUserService)

  val mockTokenRepository =
    new MockTokenRepository
  val mockTokenRepositoryLayer: ULayer[TokenRepository] =
    ZLayer.succeed(mockTokenRepository)

  val mockJwtServiceLayer: ULayer[JwtService] =
    ZLayer.succeed(new MockJwtService(mockTokenRepository))

  val testAuthServiceLayer: ZLayer[Any, Nothing, AuthService & TokenRepository] =
    ZLayer.make[AuthService & TokenRepository](
      mockUserServiceLayer,
      mockJwtServiceLayer,
      mockTokenRepositoryLayer,
      AuthServiceImpl.layer,
    )

  def spec =
    suite("AuthService")(
      test("login should return token for valid credentials") {
        for
          authService <- ZIO.service[AuthService]
          tokenOpt <-
            authService.login(unsafeEmail("existing@example.com"), unsafePassword("password123"))
        yield assertTrue(
          tokenOpt.isDefined,
          tokenOpt.map(_.userId.value).contains("existing-user"),
        )
      }.provide(
        ZLayer.make[AuthService](mockUserServiceLayer, mockJwtServiceLayer, AuthServiceImpl.layer)
      ),
      test("login should return None for invalid credentials") {
        for
          authService <- ZIO.service[AuthService]
          tokenOpt <-
            authService.login(unsafeEmail("existing@example.com"), unsafePassword("wrong-password"))
        yield assertTrue(tokenOpt.isEmpty)
      }.provide(
        ZLayer.make[AuthService](mockUserServiceLayer, mockJwtServiceLayer, AuthServiceImpl.layer)
      ),
      test("register should create user and return token") {
        for
          authService <- ZIO.service[AuthService]
          token <-
            authService.register(
              unsafeEmail("new@example.com"),
              unsafePassword("secure-password"),
              Some(unsafeFirstName("New")),
              Some(unsafeLastName("User")),
            )
        yield assertTrue(
          token.token.value.contains("access-token-for-user-"),
          token.userId.value.nonEmpty,
        )
      }.provide(
        ZLayer.make[AuthService](mockUserServiceLayer, mockJwtServiceLayer, AuthServiceImpl.layer)
      ),
      test("register should fail for existing user") {
        for
          authService <- ZIO.service[AuthService]
          result <-
            authService
              .register(
                unsafeEmail("existing@example.com"),
                unsafePassword("password"),
                None,
                None,
              )
              .exit
        yield assertTrue(result.isFailure)
      }.provide(
        ZLayer.make[AuthService](mockUserServiceLayer, mockJwtServiceLayer, AuthServiceImpl.layer)
      ),
      test("logout should remove all refresh tokens for user") {
        for
          authService <- ZIO.service[AuthService]
          tokenRepo <- ZIO.service[TokenRepository]

          userId = unsafeUserId("logout-test-user")
          jwtToken1 = unsafeJwtRefreshToken("refresh-token-1")
          jwtToken2 = unsafeJwtRefreshToken("refresh-token-2")
          refreshToken1 = RefreshToken(jwtToken1, Instant.now().plusSeconds(3600), userId)
          refreshToken2 = RefreshToken(jwtToken2, Instant.now().plusSeconds(3600), userId)
          _ <- tokenRepo.saveRefreshToken(refreshToken1)
          _ <- tokenRepo.saveRefreshToken(refreshToken2)

          _ <- authService.logout(userId)

          token1 <- tokenRepo.findByRefreshToken(jwtToken1)
          token2 <- tokenRepo.findByRefreshToken(jwtToken2)
        yield assertTrue(
          token1.isEmpty,
          token2.isEmpty,
        )
      }.provide(testAuthServiceLayer),
      test("refreshToken should create new access token") {
        for
          jwtService <- ZIO.service[JwtService]
          tokenRepo <- ZIO.service[TokenRepository]

          userId = unsafeUserId("refresh-test-user")
          jwtTestToken = unsafeJwtRefreshToken("refresh-token-test")
          refreshToken =
            RefreshToken(
              jwtTestToken,
              Instant.now().plusSeconds(3600),
              userId,
            )
          _ <- tokenRepo.saveRefreshToken(refreshToken)

          savedToken <- tokenRepo.findByRefreshToken(jwtTestToken)
          _ <- ZIO.debug(s"Saved token before refresh: $savedToken")

          accessTokenOpt <- jwtService.refreshToken(jwtTestToken)

          newTokenExists = accessTokenOpt.isDefined
          remainingToken <- tokenRepo.findByRefreshToken(jwtTestToken)
          _ <- ZIO.debug(s"Remaining token after refresh: $remainingToken")
          oldTokenDeleted = remainingToken.isEmpty
        yield assertTrue(
          newTokenExists,
          oldTokenDeleted,
        )
      }.provide(
        ZLayer.make[JwtService & TokenRepository](mockJwtServiceLayer, mockTokenRepositoryLayer)
      ),
    )
