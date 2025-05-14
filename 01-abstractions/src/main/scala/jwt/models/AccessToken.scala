package jwt.models

import java.time.Instant
import user.models.UserId
final case class AccessToken(
  token: JwtAccessToken,
  expiresAt: Instant,
  userId: UserId,
)
