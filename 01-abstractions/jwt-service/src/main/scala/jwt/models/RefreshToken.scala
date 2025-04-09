package jwt.models

import zio.json.*
import java.time.Instant

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
