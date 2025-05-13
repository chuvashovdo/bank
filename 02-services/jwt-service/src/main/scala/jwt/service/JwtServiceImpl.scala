package jwt.service

import zio.*
import jwt.config.JwtConfig
import jwt.models.{ AccessToken, JwtAccessToken, JwtRefreshToken }
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
      tokenString <-
        ZIO.attempt(
          JwtZIOJson.encode(
            claim,
            secretKey,
            JwtAlgorithm.HS256,
          )
        )
      jwtAccessToken <-
        ZIO
          .fromEither(JwtAccessToken(tokenString))
          .mapError(err =>
            new IllegalArgumentException(
              s"Failed to create JwtAccessToken: ${err.developerFriendlyMessage}"
            )
          )
    yield AccessToken(jwtAccessToken, expiresAt, userId)

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
      jwtRefreshToken <-
        ZIO
          .fromEither(JwtRefreshToken(tokenString))
          .mapError(err =>
            new IllegalArgumentException(
              s"Failed to create JwtRefreshToken: ${err.developerFriendlyMessage}"
            )
          )
      refreshToken = RefreshToken(jwtRefreshToken, expiresAt, userId)
      _ <- tokenRepository.saveRefreshToken(refreshToken)
    yield refreshToken

  override def validateToken(token: JwtAccessToken): Task[UserId] =
    for
      secretKey <- jwtConfig.secretKey
      claim <- ZIO.fromTry(JwtZIOJson.decode(token.value, secretKey, Seq(JwtAlgorithm.HS256)))
      now = Instant.now().getEpochSecond
      expiration <-
        ZIO.fromOption(claim.expiration).orElseFail(new Exception("Token has no expiration"))
      _ <- ZIO.fail(new Exception("Token expired")).when(expiration <= now)
      subject <- ZIO.fromOption(claim.subject).orElseFail(new Exception("No subject in token"))
      userId <-
        ZIO
          .fromEither(UserId(subject))
          .mapError(err =>
            new IllegalArgumentException(
              s"Invalid UserId format in token: ${err.developerFriendlyMessage}"
            )
          )
    yield userId

  override def refreshToken(token: JwtRefreshToken): Task[Option[AccessToken]] =
    for
      refreshTokenOpt <- tokenRepository.findByRefreshToken(token)
      result <-
        refreshTokenOpt match
          case Some(rToken) =>
            tokenRepository.deleteByRefreshToken(rToken.token) *>
              createAccessToken(rToken.userId).map(Some(_))
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
