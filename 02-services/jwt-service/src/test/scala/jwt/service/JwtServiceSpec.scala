package jwt.service

import zio.*
import zio.test.*
import jwt.config.JwtConfig
import user.models.UserId
import java.time.Instant
import jwt.models.{ RefreshToken, JwtAccessToken, JwtRefreshToken }
import jwt.repository.TokenRepository

object JwtServiceSpec extends ZIOSpecDefault:
  // Helper functions for creating custom types
  private def unsafeUserId(id: String): UserId =
    UserId(id).getOrElse(throw new RuntimeException(s"Invalid UserId in test setup: $id"))

  private def unsafeJwtAccessToken(token: String): JwtAccessToken =
    JwtAccessToken(token).getOrElse(
      throw new RuntimeException(s"Invalid JwtAccessToken in test setup: $token")
    )

  // Создаем мок JwtConfig
  class MockJwtConfig extends JwtConfig:
    override def secretKey: Task[String] =
      ZIO.succeed("test-secret-key-secure-needs-at-least-32-chars")
    override def accessTokenExpiration: Task[Long] =
      ZIO.succeed(60L) // 60 минут
    override def refreshTokenExpiration: Task[Long] =
      ZIO.succeed(30L) // 30 дней
    override def issuer: Task[String] =
      ZIO.succeed("test-issuer")
    override def audience: Task[String] =
      ZIO.succeed("test-audience")
    override def accessTokenExpirationMillis: Task[Long] =
      ZIO.succeed(60L * 60 * 1000) // 60 минут в мс
    override def refreshTokenExpirationMillis: Task[Long] =
      ZIO.succeed(30L * 24 * 60 * 60 * 1000) // 30 дней в мс

  // Тестовый слой
  val mockJwtConfigLayer =
    ZLayer.succeed(new MockJwtConfig)
  // Моковое хранилище для refresh-токенов
  val mockTokenRepositoryLayer =
    ZLayer.succeed:
      new TokenRepository:
        override def saveRefreshToken(token: RefreshToken): Task[Unit] =
          ZIO.unit
        override def findByRefreshToken(token: JwtRefreshToken): Task[Option[RefreshToken]] =
          ZIO.succeed(None)
        override def deleteByRefreshToken(token: JwtRefreshToken): Task[Unit] =
          ZIO.unit
        override def deleteAllByUserId(userId: UserId): Task[Unit] =
          ZIO.unit
        override def cleanExpiredTokens(): Task[Unit] =
          ZIO.unit

  val testJwtServiceLayer =
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
          userId = unsafeUserId("test-user-id")
          issuedAt = Instant.now()
          accessToken <- jwtService.createAccessToken(userId, issuedAt)
        yield assertTrue(
          accessToken.token.value.nonEmpty,
          accessToken.userId.value == userId.value,
          accessToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("createRefreshToken creates valid token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId("test-user-id")
          issuedAt = Instant.now()
          refreshToken <- jwtService.createRefreshToken(userId, issuedAt)
        yield assertTrue(
          refreshToken.token.value.nonEmpty,
          refreshToken.userId.value == userId.value,
          refreshToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("validateToken validates a correct token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = unsafeUserId("test-user-id")
          accessToken <- jwtService.createAccessToken(userId, Instant.now())
          validatedUserId <- jwtService.validateToken(accessToken.token)
        yield assertTrue(validatedUserId.value == userId.value)
      }.provide(testJwtServiceLayer),
      test("validateToken fails for expired token") {
        for
          jwtService <- ZIO.service[JwtService]
          config <- ZIO.service[MockJwtConfig]
          userId = unsafeUserId("test-user-id")
          now = Instant.now()
          expiredTime = now.minusSeconds(3600)
          secretKey <- config.secretKey
          issuer <- config.issuer
          audience <- config.audience
          claim =
            pdi
              .jwt
              .JwtClaim(
                issuer = Some(issuer),
                audience = Some(Set(audience)),
                subject = Some(userId.value),
                expiration = Some(expiredTime.toEpochMilli / 1000),
                issuedAt = Some(expiredTime.minusSeconds(3600).toEpochMilli / 1000),
              )
          tokenString = pdi.jwt.JwtZIOJson.encode(claim, secretKey, pdi.jwt.JwtAlgorithm.HS256)
          jwtToken = unsafeJwtAccessToken(tokenString)
          result <- jwtService.validateToken(jwtToken).exit
        yield assertTrue(result.isFailure)
      }.provide(testJwtServiceLayer, mockJwtConfigLayer),
      test("validateToken fails for invalid token") {
        for
          jwtService <- ZIO.service[JwtService]
          invalidTokenString = "invalid.token.format"
          invalidJwtToken = unsafeJwtAccessToken(invalidTokenString)
          result <- jwtService.validateToken(invalidJwtToken).exit
        yield assertTrue(result.isFailure)
      }.provide(testJwtServiceLayer),
    )
