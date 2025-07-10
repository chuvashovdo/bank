package user.mapper

import zio.*
import user.entity.PermissionEntity
import user.models.Permission
import user.models.PermissionId
import java.time.Instant

object PermissionMapper:
  def toModelFromEntity(entity: PermissionEntity): Task[Permission] =
    ZIO.succeed(Permission(PermissionId(entity.id), entity.name, entity.description))

  def toEntityFromModel(model: Permission): PermissionEntity =
    PermissionEntity(
      id = model.id.value,
      name = model.name,
      description = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
    )
