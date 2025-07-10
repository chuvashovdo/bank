package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }

final case class CreatePermissionRequest(
  name: String,
  description: Option[String],
)

object CreatePermissionRequest:
  given JsonDecoder[CreatePermissionRequest] =
    DeriveJsonDecoder.gen[CreatePermissionRequest]
  given JsonEncoder[CreatePermissionRequest] =
    DeriveJsonEncoder.gen[CreatePermissionRequest]
