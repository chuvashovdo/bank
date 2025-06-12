package jwt.service

import zio.*
import jwt.config.JwtConfig
import jwt.models.{ AccessToken, JwtAccessToken, JwtRefreshToken }
import user.models.UserId
import java.time.Instant
import pdi.jwt.{ JwtAlgorithm, JwtClaim, JwtZIOJson }
import jwt.models.RefreshToken
import jwt.repository.TokenRepository
import jwt.entity.RefreshTokenEntity
import java.util.UUID
import jwt.errors.{
  TokenMissingClaimError,
  TokenExpiredError,
  InvalidTokenSubjectFormatError,
  TokenDecodingError,
}

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
          subject = Some(userId.value.toString),
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
          subject = Some(userId.value.toString),
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
      domainRefreshToken <- ZIO.succeed(RefreshToken(jwtRefreshToken, expiresAt, userId))

      entityId <- ZIO.succeed(UUID.randomUUID())
      currentTime <- ZIO.succeed(Instant.now())

      refreshTokenEntityToSave <-
        ZIO.succeed:
          RefreshTokenEntity(
            id = entityId,
            userId = domainRefreshToken.userId.value,
            refreshToken = domainRefreshToken.token.value,
            expiresAt = domainRefreshToken.expiresAt,
            createdAt = currentTime,
          )
      _ <- tokenRepository.saveRefreshToken(refreshTokenEntityToSave)
    yield domainRefreshToken

  override def validateToken(token: JwtAccessToken): Task[UserId] =
    for
      secretKey <- jwtConfig.secretKey
      claim <-
        ZIO
          .fromTry(JwtZIOJson.decode(token.value, secretKey, Seq(JwtAlgorithm.HS256)))
          .mapError(err => TokenDecodingError(details = Some(err.getMessage)))
      now = Instant.now().getEpochSecond
      expiration <-
        ZIO.fromOption(claim.expiration).orElseFail(TokenMissingClaimError("expiration"))
      _ <-
        ZIO
          .fail(TokenExpiredError(expiredAt = Some(expiration), now = Some(now)))
          .when(expiration <= now)
      subject <- ZIO.fromOption(claim.subject).orElseFail(TokenMissingClaimError("subject"))
      userId <-
        ZIO
          .attempt(UUID.fromString(subject))
          .map(UserId(_))
          .mapError(err => InvalidTokenSubjectFormatError(subject, err.getMessage))
    yield userId

  override def renewAccessToken(token: JwtRefreshToken): Task[AccessToken] =
    for
      refreshToken <- tokenRepository.findByRefreshToken(token.value)
      _ <- tokenRepository.deleteByRefreshToken(refreshToken.token.value)
      accessToken <- createAccessToken(refreshToken.userId)
    yield accessToken

  override def invalidateRefreshTokens(userId: UserId): Task[Unit] =
    tokenRepository.deleteAllByUserId(userId.value)

object JwtServiceImpl:
  val layer: ZLayer[JwtConfig & TokenRepository, Nothing, JwtService] =
    ZLayer:
      for
        jwtConfig <- ZIO.service[JwtConfig]
        tokenRepository <- ZIO.service[TokenRepository]
      yield JwtServiceImpl(jwtConfig, tokenRepository)
