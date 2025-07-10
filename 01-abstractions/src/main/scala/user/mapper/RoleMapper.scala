package user.mapper

import zio.*
import user.entity.RoleEntity
import user.models.{ Permission, Role, RoleId }
import java.time.Instant

object RoleMapper:
  def toModelFromEntity(entity: RoleEntity, permissions: Set[Permission]): Task[Role] =
    ZIO.succeed(Role(RoleId(entity.id), entity.name, entity.description, permissions))

  def toEntityFromModel(model: Role): RoleEntity =
    RoleEntity(
      id = model.id.value,
      name = model.name,
      description = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
    )
