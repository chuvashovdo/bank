package jwt.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.durationInt
import jwt.config.JwtConfig
import user.models.UserId
import java.time.Instant
import jwt.models.{ RefreshToken, JwtAccessToken, JwtRefreshToken }
import jwt.repository.TokenRepository
import jwt.entity.RefreshTokenEntity
import auth.errors.RefreshTokenNotFoundError
import java.util.UUID
import jwt.errors.TokenExpiredError

object JwtServiceSpec extends ZIOSpecDefault:
  // Helper functions for creating custom types
  private def unsafeUserId(): UserId =
    UserId(UUID.randomUUID())

  private def unsafeJwtAccessToken(token: String): JwtAccessToken =
    JwtAccessToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtAccessToken in test setup: $token")
    )

  // Создаем мок JwtConfig
  private val mockJwtConfig =
    JwtConfig(
      secretKey = "test-secret-key-secure-needs-at-least-32-chars",
      accessTokenExpiration = 60.minutes,
      refreshTokenExpiration = 30.days,
      issuer = "test-issuer",
      audience = "test-audience",
    )

  // Тестовый слой
  val mockJwtConfigLayer: ULayer[JwtConfig] =
    ZLayer.succeed(mockJwtConfig)

  // Моковое хранилище для refresh-токенов
  val mockTokenRepositoryLayer: ULayer[TokenRepository] =
    ZLayer.succeed:
      new TokenRepository:
        override def saveRefreshToken(token: RefreshTokenEntity): Task[Unit] =
          ZIO.unit
        override def findByRefreshToken(token: String): Task[RefreshToken] =
          ZIO.fail(RefreshTokenNotFoundError(token))
        override def deleteByRefreshToken(token: String): Task[Unit] =
          ZIO.unit
        override def deleteAllByUserId(userId: UUID): Task[Unit] =
          ZIO.unit
        override def cleanExpiredTokens(): Task[Unit] =
          ZIO.unit

  val testJwtServiceLayer: ULayer[JwtService] =
    ZLayer.make[JwtService](
      mockJwtConfigLayer,
      mockTokenRepositoryLayer,
      JwtServiceImpl.layer,
    )

  def spec =
    suite("JwtService")(
      test("createAccessToken creates valid token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId()
          issuedAt = Instant.now()
          accessToken <- jwtService.createAccessToken(userId, issuedAt)
        yield assertTrue(
          accessToken.token.value.nonEmpty,
          accessToken.userId == userId,
          accessToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("createRefreshToken creates valid token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId()
          issuedAt = Instant.now()
          refreshToken <- jwtService.createRefreshToken(userId, issuedAt)
        yield assertTrue(
          refreshToken.token.value.nonEmpty,
          refreshToken.userId == userId,
          refreshToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("validateToken validates a correct token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId()
          accessToken <- jwtService.createAccessToken(userId, Instant.now())
          validatedUserId <- jwtService.validateToken(accessToken.token)
        yield assertTrue(validatedUserId == userId)
      }.provide(testJwtServiceLayer),
      test("validateToken fails for expired token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId()
          now = Instant.now()
          expiredTime = now.minusSeconds(3600)
          claim =
            pdi
              .jwt
              .JwtClaim(
                issuer = Some(mockJwtConfig.issuer),
                audience = Some(Set(mockJwtConfig.audience)),
                subject = Some(userId.value.toString),
                expiration = Some(expiredTime.toEpochMilli / 1000),
                issuedAt = Some(expiredTime.minusSeconds(3600).toEpochMilli / 1000),
              )
          tokenString =
            pdi.jwt.JwtZIOJson.encode(claim, mockJwtConfig.secretKey, pdi.jwt.JwtAlgorithm.HS256)
          jwtToken = unsafeJwtAccessToken(tokenString)
          result <- jwtService.validateToken(jwtToken).exit
        yield assert(result)(fails(isSubtype[TokenExpiredError](Assertion.anything)))
      }.provide(testJwtServiceLayer),
      test("validateToken fails for invalid token") {
        for
          jwtService <- ZIO.service[JwtService]
          invalidTokenString = "a.b.c"
          invalidJwtToken = unsafeJwtAccessToken(invalidTokenString)
          result <- jwtService.validateToken(invalidJwtToken).exit
        yield assertTrue(result.isFailure)
      }.provide(testJwtServiceLayer),
    )
