package jwt.models

import user.models.{ UserId, Role, Permission }
import zio.json.*

final case class AuthContext(
  userId: UserId,
  roles: Set[Role],
  permissions: Set[Permission],
)

object AuthContext:
  given JsonCodec[AuthContext] =
    DeriveJsonCodec.gen[AuthContext]
