package user.entity

import java.time.Instant

final case class UserEntity(
  id: String,
  email: String,
  passwordHash: String,
  firstName: Option[String],
  lastName: Option[String],
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
)
