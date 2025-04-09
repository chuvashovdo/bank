package jwt.models

import zio.json.*
import java.time.Instant

final case class AccessToken(
  token: String,
  expiresAt: Instant,
  userId: UserId,
)

// object AccessToken:
//   given JsonCodec[AccessToken] =
//     DeriveJsonCodec.gen[AccessToken]
//   given JsonCodec[Instant] =
//     JsonCodec.instant
