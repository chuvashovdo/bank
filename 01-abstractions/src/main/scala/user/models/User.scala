package user.models

import zio.json.*
final case class User(
  id: UserId,
  email: String,
  passwordHash: String,
  firstName: Option[String],
  lastName: Option[String],
  isActive: Boolean,
)

object User:
  given JsonCodec[User] =
    DeriveJsonCodec.gen[User]
