package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }
import user.models.PermissionId

final case class UpdatePermissionRequest(
  id: PermissionId,
  name: String,
  description: Option[String],
)

object UpdatePermissionRequest:
  given JsonDecoder[UpdatePermissionRequest] =
    DeriveJsonDecoder.gen[UpdatePermissionRequest]
  given JsonEncoder[UpdatePermissionRequest] =
    DeriveJsonEncoder.gen[UpdatePermissionRequest]
