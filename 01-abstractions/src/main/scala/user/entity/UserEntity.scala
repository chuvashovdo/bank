package user.entity

import java.time.Instant
import java.util.UUID

final case class UserEntity(
  id: UUID,
  email: String,
  passwordHash: String,
  firstName: Option[String],
  lastName: Option[String],
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
)
