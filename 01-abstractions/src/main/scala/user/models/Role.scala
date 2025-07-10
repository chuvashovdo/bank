package user.models

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Role(
  id: RoleId,
  name: String,
  description: Option[String],
  permissions: Set[Permission],
)

object Role:
  given codec: JsonCodec[Role] =
    DeriveJsonCodec.gen[Role]
