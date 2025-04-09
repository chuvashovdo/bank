package jwt.service

import zio.*
import jwt.config.JwtConfig
import jwt.models.AccessToken
import user.models.UserId
import java.time.Instant
import pdi.jwt.{ JwtAlgorithm, JwtClaim, JwtZIOJson }
import jwt.entity.RefreshTokenEntity
import jwt.models.RefreshToken
import zio.json.*
class JwtServiceImpl(jwtConfig: JwtConfig) extends JwtService:
  override def createAccessToken(
    userId: UserId,
    issuedAt: Instant = Instant.now(),
  ): Task[AccessToken] =
    for
      expirationMillis <- jwtConfig.accessTokenExpirationMillis
      issuer <- jwtConfig.issuer
      audience <- jwtConfig.audience
      secretKey <- jwtConfig.secretKey
      expiresAt = issuedAt.plusMillis(expirationMillis)
      claim =
        JwtClaim(
          content = s"""{"userId": "${userId.value}"}""",
          issuer = Some(issuer),
          audience = Some(Set(audience)),
          subject = Some(userId.value),
          expiration = Some(expiresAt.toEpochMilli),
          issuedAt = Some(issuedAt.toEpochMilli),
        )
      token <-
        ZIO.attempt(
          JwtZIOJson.encode(
            claim,
            secretKey,
            JwtAlgorithm.HS256,
          )
        )
    yield AccessToken(token, expiresAt, userId)

  override def createRefreshToken(
    userId: UserId,
    issuedAt: Instant = Instant.now(),
  ): Task[RefreshToken] =
    for
      expirationMillis <- jwtConfig.refreshTokenExpirationMillis
      issuer <- jwtConfig.issuer
      audience <- jwtConfig.audience
      secretKey <- jwtConfig.secretKey
      expiresAt = issuedAt.plusMillis(expirationMillis)
      claim =
        JwtClaim(
          content = s"""{"userId": "${userId.value}"}""",
          issuer = Some(issuer),
          audience = Some(Set(audience)),
          subject = Some(userId.value),
          expiration = Some(expiresAt.toEpochMilli),
          issuedAt = Some(issuedAt.toEpochMilli),
        )
      token <-
        ZIO.attempt(
          JwtZIOJson.encode(
            claim,
            secretKey,
            JwtAlgorithm.HS256,
          )
        )
    yield RefreshToken(token, expiresAt, userId)

  override def validateToken(token: String): Task[UserId] =
    for
      secretKey <- jwtConfig.secretKey
      claim <- ZIO.fromTry(JwtZIOJson.decode(token, secretKey, Seq(JwtAlgorithm.HS256)))
      now = Instant.now().getEpochSecond
      expiration <-
        ZIO.fromOption(claim.expiration).orElseFail(new Exception("Token has no expiration"))
      _ <- ZIO.fail(new Exception("Token expired")).when(expiration <= now)
      content = claim.content
      jwtContent <-
        ZIO
          .fromEither(content.fromJson[JwtContent])
          .mapError(_ => new Exception("Invalid token content"))
    yield UserId(jwtContent.userId)

  case class JwtContent(userId: String) derives JsonDecoder

  override def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: String,
    expiresAt: Instant,
  ): Task[RefreshTokenEntity] =
    ZIO.succeed(RefreshTokenEntity(id, userId.value, refreshToken, expiresAt, Instant.now()))

object JwtServiceImpl:
  val layer: ZLayer[JwtConfig, Nothing, JwtService] =
    ZLayer.fromFunction(JwtServiceImpl(_))
