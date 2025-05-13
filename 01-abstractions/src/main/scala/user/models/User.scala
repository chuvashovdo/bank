package user.models

import zio.json.*

final case class User(
  id: UserId,
  email: Email,
  passwordHash: String,
  firstName: Option[FirstName],
  lastName: Option[LastName],
  isActive: Boolean,
)

object User:
  given JsonCodec[User] =
    DeriveJsonCodec.gen[User]
