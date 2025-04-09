package jwt.service

trait JwtService:
  def createAccessToken(userId: UserId, issuedAt: Instant): Task[AccessToken]
  def createRefreshToken(userId: UserId, issuedAt: Instant): Task[RefreshToken]
  def validateToken(token: String): Task[UserId]
  def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: String,
    expiresAt: Instant,
  ): Task[RefreshTokenEntity]

object JwtService:
  def createAccessToken(userId: UserId): ZIO[JwtService, Nothing, AccessToken] =
    ZIO.serviceWithZIO[JwtService](_.createAccessToken(userId))
  def createRefreshToken(userId: UserId): ZIO[JwtService, Nothing, RefreshToken] =
    ZIO.serviceWithZIO[JwtService](_.createRefreshToken(userId))
  def validateToken(token: String): ZIO[JwtService, Nothing, UserId] =
    ZIO.serviceWithZIO[JwtService](_.validateToken(token))
  def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: String,
    expiresAt: Instant,
  ): ZIO[JwtService, Nothing, RefreshTokenEntity] =
    ZIO.serviceWithZIO[JwtService](
      _.createRefreshTokenEntity(id, userId, refreshToken, expiresAt)
    )
