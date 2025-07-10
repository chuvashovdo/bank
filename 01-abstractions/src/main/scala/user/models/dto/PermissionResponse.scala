package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }
import user.models.PermissionId

final case class PermissionResponse(
  id: PermissionId,
  name: String,
  description: Option[String],
)

object PermissionResponse:
  given JsonDecoder[PermissionResponse] =
    DeriveJsonDecoder.gen[PermissionResponse]
  given JsonEncoder[PermissionResponse] =
    DeriveJsonEncoder.gen[PermissionResponse]
