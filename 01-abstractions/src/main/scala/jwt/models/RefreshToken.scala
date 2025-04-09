package jwt

-service.models

import zio.json.*
import java.time.Instant
import user.models.UserId

final case class RefreshToken(
  token: String,
  expiresAt: Instant,
  userId: UserId,
)

// object RefreshToken:
//   given JsonCodec[RefreshToken] =
//     DeriveJsonCodec.gen[RefreshToken]
//   given JsonCodec[Instant] =
//     JsonCodec.instant
