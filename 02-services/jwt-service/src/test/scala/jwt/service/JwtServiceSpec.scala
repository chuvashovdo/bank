package jwt.service

import zio.*
import zio.test.*
import jwt.config.JwtConfig
import user.models.UserId
import java.time.Instant

object JwtServiceSpec extends ZIOSpecDefault:
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
  val testJwtServiceLayer =
    ZLayer.make[JwtService](
      mockJwtConfigLayer,
      JwtServiceImpl.layer,
    )

  def spec =
    suite("JwtService")(
      test("createAccessToken creates valid token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = UserId("test-user-id")
          issuedAt = Instant.now()
          accessToken <- jwtService.createAccessToken(userId, issuedAt)
        yield assertTrue(
          accessToken.token.nonEmpty,
          accessToken.userId.value == userId.value,
          accessToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("createRefreshToken creates valid token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = UserId("test-user-id")
          issuedAt = Instant.now()
          refreshToken <- jwtService.createRefreshToken(userId, issuedAt)
        yield assertTrue(
          refreshToken.token.nonEmpty,
          refreshToken.userId.value == userId.value,
          refreshToken.expiresAt.isAfter(issuedAt),
        )
      }.provide(testJwtServiceLayer),
      test("validateToken validates a correct token") {
        for
          jwtService <- ZIO.service[JwtService]
          userId = UserId("test-user-id")
          accessToken <- jwtService.createAccessToken(userId, Instant.now())
          validatedUserId <- jwtService.validateToken(accessToken.token)
        yield assertTrue(validatedUserId.value == userId.value)
      }.provide(testJwtServiceLayer),
      test("validateToken fails for expired token") {
        for
          jwtService <- ZIO.service[JwtService]
          config <- ZIO.service[JwtConfig]
          userId = UserId("test-user-id")
          // Создаем токен со сроком действия в прошлом
          // При этом устанавливаем срок действия напрямую в JwtClaim,
          // чтобы имитировать истекший токен
          now = Instant.now()
          expiredTime = now.minusSeconds(3600) // час назад
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
                expiration = Some(expiredTime.toEpochMilli / 1000), // в секундах
                issuedAt = Some(expiredTime.minusSeconds(3600).toEpochMilli / 1000),
              )
          token = pdi.jwt.JwtZIOJson.encode(claim, secretKey, pdi.jwt.JwtAlgorithm.HS256)
          result <- jwtService.validateToken(token).exit
        yield assertTrue(result.isFailure)
      }.provide(
        ZLayer.succeed(new MockJwtConfig),
        JwtServiceImpl.layer,
      ),
      test("validateToken fails for invalid token") {
        for
          jwtService <- ZIO.service[JwtService]
          invalidToken = "invalid.token.format"
          result <- jwtService.validateToken(invalidToken).exit
        yield assertTrue(result.isFailure)
      }.provide(testJwtServiceLayer),
    )
