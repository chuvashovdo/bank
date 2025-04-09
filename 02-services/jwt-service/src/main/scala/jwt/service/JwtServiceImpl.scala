package jwt.service

import zio.*
import jwt.config.JwtConfig
class JwtServiceImpl(jwtConfig: JwtConfig) extends JwtService:
  override def createAccessToken(
    userId: UserId,
    issuedAt: Instant = Instant.now(),
  ): Task[AccessToken] =
    val expiresAt =
      issuedAt.plus(
        jwtConfig.accessTokenExpirationMillis.getOrFail("Access token expiration is not set")
      )
    val claim =
      JwtClaim(
        content = s"""{"userId": "${userId.value}"}""",
        issuer = jwtConfig.issuer.getOrFail("Issuer is not set"),
        audience = jwtConfig.audience.getOrFail("Audience is not set"),
        subject = userId.value,
        expiration = Some(expiresAt.toEpochMilli),
        issuedAt = Some(issuedAt.toEpochMilli),
      )
    val token =
      JwtZIOJson.encode(
        claim,
        jwtConfig.secretKey.getOrFail("Secret key is not set"),
        JwtAlgorithm.HS256,
      )
    AccessToken(token, expiresAt, userId)
