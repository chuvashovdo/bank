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
import jwt.entity.RefreshTokenEntity
import common.errors.InvalidCredentialsError
import common.errors.RefreshTokenNotFoundError
import common.errors.UserNotFoundError
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

    override def findUserById(id: UserId): Task[User] =
      ZIO
        .attempt(users.values.find(_.id.equals(id)).get)
        .mapError(_ => new UserNotFoundError(id.value))

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

    override def validateCredentials(email: Email, password: Password): Task[User] =
      for
        user <- findUserByEmail(email)
        _ <- ZIO.fail(InvalidCredentialsError()).when(password.value != "password123")
      yield user

    override def updateUser(
      id: UserId,
      firstName: Option[FirstName],
      lastName: Option[LastName],
    ): Task[User] =
      ZIO.fail(new RuntimeException("Not implemented"))

    override def changePassword(
      id: UserId,
      oldPassword: Password,
      newPassword: Password,
    ): Task[Unit] =
      ZIO.fail(new RuntimeException("Not implemented"))

    override def deactivateUser(id: UserId): Task[Unit] =
      ZIO.fail(new RuntimeException("Not implemented"))

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
      val refreshToken = RefreshToken(jwtRefreshToken, issuedAt.plusSeconds(86400), userId)
      val refreshTokenEntity =
        RefreshTokenEntity(
          id = s"refresh-token-${userId.value}",
          userId = refreshToken.userId.value,
          refreshToken = refreshToken.token.value,
          expiresAt = refreshToken.expiresAt,
          createdAt = Instant.now(),
        )
      tokenRepository.saveRefreshToken(refreshTokenEntity)
      ZIO.succeed(refreshToken)
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

    override def renewAccessToken(token: JwtRefreshToken): Task[AccessToken] =
      for
        refreshToken <- tokenRepository.findByRefreshToken(token.value)
        result <-
          for
            _ <- tokenRepository.deleteByRefreshToken(token.value)
            accessToken <- createAccessToken(refreshToken.userId, Instant.now())
          yield accessToken
      yield result

    override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
      tokenRepository.deleteAllByUserId(userId.value)

  class MockTokenRepository extends TokenRepository:
    private var tokens: Map[String, RefreshToken] =
      Map.empty

    override def saveRefreshToken(tokenEntity: RefreshTokenEntity): Task[Unit] =
      ZIO.succeed:
        tokens =
          tokens + (tokenEntity.refreshToken -> RefreshToken(
            token = unsafeJwtRefreshToken(tokenEntity.refreshToken),
            expiresAt = tokenEntity.expiresAt,
            userId = unsafeUserId(tokenEntity.userId),
          ))

    override def findByRefreshToken(refreshTokenKey: String): Task[RefreshToken] =
      tokens.get(refreshTokenKey) match
        case Some(token) => ZIO.succeed(token)
        case None => ZIO.fail(RefreshTokenNotFoundError(refreshTokenKey))

    override def deleteByRefreshToken(refreshTokenString: String): Task[Unit] =
      for _ <- ZIO.succeed { tokens = tokens - refreshTokenString } yield ()

    override def deleteAllByUserId(userId: String): Task[Unit] =
      ZIO.succeed:
        tokens = tokens.filter { case (_, token) => token.userId.value != userId }
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
          token <-
            authService.login(unsafeEmail("existing@example.com"), unsafePassword("password123"))
        yield assertTrue(
          token.token.value.nonEmpty
        )
      }.provide(
        ZLayer.make[AuthService](mockUserServiceLayer, mockJwtServiceLayer, AuthServiceImpl.layer)
      ),
      test("login should return None for invalid credentials") {
        for
          authService <- ZIO.service[AuthService]
          result <-
            authService
              .login(unsafeEmail("existing@example.com"), unsafePassword("wrong-password"))
              .exit
        yield assertTrue(result.isFailure)
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
          refreshTokenEntity1 =
            RefreshTokenEntity(
              id = s"refresh-token-${userId.value}",
              userId = refreshToken1.userId.value,
              refreshToken = refreshToken1.token.value,
              expiresAt = refreshToken1.expiresAt,
              createdAt = Instant.now(),
            )
          _ <- tokenRepo.saveRefreshToken(refreshTokenEntity1)

          _ <- authService.logout(userId)

          token1 <- tokenRepo.findByRefreshToken(refreshToken1.token.value).exit
          token2 <- tokenRepo.findByRefreshToken(refreshToken2.token.value).exit
        yield assertTrue(
          token1.isFailure && token2.isFailure
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
          refreshTokenEntity =
            RefreshTokenEntity(
              id = s"refresh-token-${userId.value}",
              userId = refreshToken.userId.value,
              refreshToken = refreshToken.token.value,
              expiresAt = refreshToken.expiresAt,
              createdAt = Instant.now(),
            )
          _ <- tokenRepo.saveRefreshToken(refreshTokenEntity)
          savedToken <- tokenRepo.findByRefreshToken(refreshToken.token.value)
          _ <- ZIO.debug(s"Saved token before refresh: $savedToken")

          accessToken <- jwtService.renewAccessToken(refreshToken.token)

          newTokenExists = accessToken.token.value.nonEmpty
          remainingToken <- tokenRepo.findByRefreshToken(refreshToken.token.value).exit
          _ <- ZIO.debug(s"Remaining token after refresh: $remainingToken")
          oldTokenDeleted = remainingToken.isFailure
        yield assertTrue(
          newTokenExists,
          oldTokenDeleted,
        )
      }.provide(
        ZLayer.make[JwtService & TokenRepository](mockJwtServiceLayer, mockTokenRepositoryLayer)
      ),
    )
