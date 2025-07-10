package user.models

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Permission(
  id: PermissionId,
  name: String,
  description: Option[String],
)

object Permission:
  given codec: JsonCodec[Permission] =
    DeriveJsonCodec.gen[Permission]
