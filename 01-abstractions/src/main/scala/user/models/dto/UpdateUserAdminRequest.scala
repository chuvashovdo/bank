package user.models.dto

import zio.json.{ JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder }
import user.models.{ UserId, Email, FirstName, LastName, RoleId }

final case class UpdateUserAdminRequest(
  id: UserId,
  email: Option[Email],
  firstName: Option[FirstName],
  lastName: Option[LastName],
  roles: Option[Set[RoleId]],
)

object UpdateUserAdminRequest:
  given JsonDecoder[UpdateUserAdminRequest] =
    DeriveJsonDecoder.gen[UpdateUserAdminRequest]
  given JsonEncoder[UpdateUserAdminRequest] =
    DeriveJsonEncoder.gen[UpdateUserAdminRequest]
