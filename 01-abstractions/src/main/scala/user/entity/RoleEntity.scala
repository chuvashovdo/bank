package user.entity

import java.time.Instant
import java.util.UUID

final case class RoleEntity(
  id: UUID,
  name: String,
  description: Option[String],
  createdAt: Instant,
  updatedAt: Instant,
)
