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
import user.errors.InvalidCredentialsError
import auth.errors.RefreshTokenNotFoundError
import user.errors.UserNotFoundError
import java.util.UUID

object AuthServiceSpec extends ZIOSpecDefault:
  private val existingUserId =
    UUID.randomUUID()

  private def unsafeUserId(id: UUID): UserId =
    UserId(id)
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
    private def internalUnsafeUserId(id: UUID): UserId =
      UserId(id)
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
          internalUnsafeUserId(existingUserId),
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
            internalUnsafeUserId(UUID.randomUUID()),
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
          id = UUID.randomUUID(),
          userId = refreshToken.userId.value,
          refreshToken = refreshToken.token.value,
          expiresAt = refreshToken.expiresAt,
          createdAt = Instant.now(),
        )
      tokenRepository.saveRefreshToken(refreshTokenEntity).as(refreshToken)

    override def validateToken(token: JwtAccessToken): Task[UserId] =
      validTokens.find { case (jwtAccessToken, _) => jwtAccessToken.value == token.value } match
        case Some((_, userId)) => ZIO.succeed(userId)
        case None =>
          if token.value.startsWith("valid-user-id-from-string-token") then
            ZIO.succeed(unsafeUserId(UUID.randomUUID()))
          else if token.value.startsWith("invalid-token") then
            ZIO.fail(new IllegalArgumentException("Invalid token"))
          else if token.value.startsWith("expired-token") then
            ZIO.fail(new IllegalArgumentException("Token expired"))
          else ZIO.fail(new IllegalArgumentException("Unknown token"))

    override def renewAccessToken(token: JwtRefreshToken): Task[AccessToken] =
      for
        refreshToken <- tokenRepository.findByRefreshToken(token.value)
        _ <- tokenRepository.deleteByRefreshToken(refreshToken.token.value)
        accessToken <- createAccessToken(refreshToken.userId, Instant.now())
      yield accessToken

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
      ZIO.succeed { tokens = tokens - refreshTokenString }

    override def deleteAllByUserId(userId: UUID): Task[Unit] =
      ZIO.succeed:
        tokens =
          tokens.filter:
            case (_, token) =>
              !token.userId.value.equals(userId)
    override def cleanExpiredTokens(): Task[Unit] =
      ZIO.succeed(())

  val mockUserServiceLayer: ULayer[UserService] =
    ZLayer.succeed(new MockUserService)

  def spec =
    suite("AuthService")(
      test("login should return token for valid credentials") {
        for
          authService <- ZIO.service[AuthService]
          token <-
            authService.login(unsafeEmail("existing@example.com"), unsafePassword("password123"))
        yield assertTrue(token.token.value.nonEmpty)
      }.provide {
        ZLayer.make[AuthService](
          mockUserServiceLayer,
          ZLayer(ZIO.succeed(new MockJwtService(new MockTokenRepository))),
          AuthServiceImpl.layer,
        )
      },
      test("login should return None for invalid credentials") {
        for
          authService <- ZIO.service[AuthService]
          result <-
            authService
              .login(unsafeEmail("existing@example.com"), unsafePassword("wrong-password"))
              .exit
        yield assertTrue(result.isFailure)
      }.provide {
        ZLayer.make[AuthService](
          mockUserServiceLayer,
          ZLayer(ZIO.succeed(new MockJwtService(new MockTokenRepository))),
          AuthServiceImpl.layer,
        )
      },
      test("register should create user and return token") {
        for
          authService <- ZIO.service[AuthService]
          token <-
            authService.register(
              unsafeEmail("new@example.com"),
              unsafePassword("password"),
              Some(unsafeFirstName("New")),
              Some(unsafeLastName("User")),
            )
        yield assertTrue(token.token.value.nonEmpty)
      }.provide {
        ZLayer.make[AuthService](
          mockUserServiceLayer,
          ZLayer(ZIO.succeed(new MockJwtService(new MockTokenRepository))),
          AuthServiceImpl.layer,
        )
      },
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
      }.provide {
        ZLayer.make[AuthService](
          mockUserServiceLayer,
          ZLayer(ZIO.succeed(new MockJwtService(new MockTokenRepository))),
          AuthServiceImpl.layer,
        )
      },
      test("logout should invalidate tokens") {
        for
          authService <- ZIO.service[AuthService]
          jwtService <- ZIO.service[JwtService]
          tokenRepo <- ZIO.service[TokenRepository]
          userId = unsafeUserId(existingUserId)
          refreshToken <- jwtService.createRefreshToken(userId, Instant.now())
          _ <- authService.logout(userId)
          foundToken <- tokenRepo.findByRefreshToken(refreshToken.token.value).exit
        yield assertTrue(foundToken.isFailure)
      }.provide {
        val mockRepo = new MockTokenRepository
        ZLayer.make[AuthService & JwtService & TokenRepository](
          mockUserServiceLayer,
          ZLayer(ZIO.succeed(new MockJwtService(mockRepo))),
          ZLayer.succeed(mockRepo),
          AuthServiceImpl.layer,
        )
      },
      test("refreshToken should create new access token") {
        for
          jwtService <- ZIO.service[JwtService]
          tokenRepo <- ZIO.service[TokenRepository]
          userId = unsafeUserId(UUID.randomUUID())
          refreshToken <- jwtService.createRefreshToken(userId, Instant.now())
          newAccessToken <- jwtService.renewAccessToken(refreshToken.token)
          oldToken <- tokenRepo.findByRefreshToken(refreshToken.token.value).exit
        yield assertTrue(newAccessToken.token.value.nonEmpty) && assertTrue(oldToken.isFailure)
      }.provide {
        val mockRepo = new MockTokenRepository
        ZLayer.make[JwtService & TokenRepository](
          ZLayer.succeed(mockRepo),
          ZLayer(ZIO.succeed(new MockJwtService(mockRepo))),
        )
      },
    )

end AuthServiceSpec
