package jwt.models

import java.time.Instant
import user.models.UserId

final case class RefreshToken(
  token: JwtRefreshToken,
  expiresAt: Instant,
  userId: UserId,
)
