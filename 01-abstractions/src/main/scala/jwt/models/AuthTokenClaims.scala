package jwt.models

import zio.json.*

final case class AuthTokenClaims(
  roles: Set[String],
  permissions: Set[String],
)

object AuthTokenClaims:
  given codec: JsonCodec[AuthTokenClaims] =
    DeriveJsonCodec.gen[AuthTokenClaims]
