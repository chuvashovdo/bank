package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }

final case class CreateRoleRequest(
  name: String,
  description: Option[String],
)

object CreateRoleRequest:
  given JsonDecoder[CreateRoleRequest] =
    DeriveJsonDecoder.gen[CreateRoleRequest]
  given JsonEncoder[CreateRoleRequest] =
    DeriveJsonEncoder.gen[CreateRoleRequest]
