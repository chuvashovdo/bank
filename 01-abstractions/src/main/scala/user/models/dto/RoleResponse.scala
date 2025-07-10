package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }
import user.models.RoleId

final case class RoleResponse(
  id: RoleId,
  name: String,
  description: Option[String],
  permissions: List[PermissionResponse],
)

object RoleResponse:
  given JsonDecoder[RoleResponse] =
    DeriveJsonDecoder.gen[RoleResponse]
  given JsonEncoder[RoleResponse] =
    DeriveJsonEncoder.gen[RoleResponse]
