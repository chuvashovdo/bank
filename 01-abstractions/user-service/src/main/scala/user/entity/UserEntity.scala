package user.entity

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
