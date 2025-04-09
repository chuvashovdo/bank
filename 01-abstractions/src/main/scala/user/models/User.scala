package user.models

final case class User(
  id: UserId,
  email: String,
  passwordHash: String,
  firstName: String,
  lastName: String,
)
