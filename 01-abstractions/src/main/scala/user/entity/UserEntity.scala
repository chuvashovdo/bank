package user.entity

import java.time.Instant
import user.models.UserId

final case class UserEntity(
  id: UserId,
  email: String,
  passwordHash: String,
  firstName: String,
  lastName: String,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
)
