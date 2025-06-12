package jwt.entity

import java.time.Instant
import java.util.UUID

final case class RefreshTokenEntity(
  id: UUID,
  userId: UUID,
  refreshToken: String,
  expiresAt: Instant,
  createdAt: Instant,
)
