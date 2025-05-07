package jwt.service

import zio.*
import jwt.config.JwtConfig
import jwt.models.AccessToken
import user.models.UserId
import java.time.Instant
import pdi.jwt.{ JwtAlgorithm, JwtClaim, JwtZIOJson }
import jwt.models.RefreshToken
import jwt.repository.TokenRepository

class JwtServiceImpl(jwtConfig: JwtConfig, tokenRepository: TokenRepository) extends JwtService:
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
          issuer = Some(issuer),
          audience = Some(Set(audience)),
          subject = Some(userId.value),
          expiration = Some(expiresAt.toEpochMilli),
          issuedAt = Some(issuedAt.toEpochMilli),
        )
      tokenString <-
        ZIO.attempt(
          JwtZIOJson.encode(
            claim,
            secretKey,
            JwtAlgorithm.HS256,
          )
        )
      refreshToken = RefreshToken(tokenString, expiresAt, userId)
      _ <- tokenRepository.saveRefreshToken(refreshToken)
    yield refreshToken

  override def validateToken(token: String): Task[UserId] =
    for
      secretKey <- jwtConfig.secretKey
      claim <- ZIO.fromTry(JwtZIOJson.decode(token, secretKey, Seq(JwtAlgorithm.HS256)))
      now = Instant.now().getEpochSecond
      expiration <-
        ZIO.fromOption(claim.expiration).orElseFail(new Exception("Token has no expiration"))
      _ <- ZIO.fail(new Exception("Token expired")).when(expiration <= now)
      subject <- ZIO.fromOption(claim.subject).orElseFail(new Exception("No subject in token"))
    yield UserId(subject)

  override def refreshToken(refreshTokenStr: String): Task[Option[AccessToken]] =
    for
      refreshTokenOpt <- tokenRepository.findByRefreshToken(refreshTokenStr)
      result <-
        refreshTokenOpt match
          case Some(rToken) =>
            for
              _ <- tokenRepository.deleteByRefreshToken(refreshTokenStr)
              accessToken <- createAccessToken(rToken.userId, Instant.now())
            yield Some(accessToken)
          case None =>
            ZIO.succeed(None)
    yield result

  override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
    tokenRepository.deleteAllByUserId(userId)

object JwtServiceImpl:
  val layer: ZLayer[JwtConfig & TokenRepository, Nothing, JwtService] =
    ZLayer:
      for
        jwtConfig <- ZIO.service[JwtConfig]
        tokenRepository <- ZIO.service[TokenRepository]
      yield JwtServiceImpl(jwtConfig, tokenRepository)
