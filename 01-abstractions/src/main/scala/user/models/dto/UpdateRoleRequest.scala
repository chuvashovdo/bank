package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }
import user.models.RoleId

final case class UpdateRoleRequest(
  id: RoleId,
  name: String,
  description: Option[String],
)

object UpdateRoleRequest:
  given JsonDecoder[UpdateRoleRequest] =
    DeriveJsonDecoder.gen[UpdateRoleRequest]
  given JsonEncoder[UpdateRoleRequest] =
    DeriveJsonEncoder.gen[UpdateRoleRequest]
