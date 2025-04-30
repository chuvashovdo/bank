package jwt.models

import java.time.Instant
import user.models.UserId
final case class AccessToken(
  token: String,
  expiresAt: Instant,
  userId: UserId,
)
