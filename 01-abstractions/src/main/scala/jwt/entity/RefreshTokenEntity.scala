package jwt.entity

import java.time.Instant

final case class RefreshTokenEntity(
  id: String,
  userId: String,
  refreshToken: String,
  expiresAt: Instant,
  createdAt: Instant,
)
