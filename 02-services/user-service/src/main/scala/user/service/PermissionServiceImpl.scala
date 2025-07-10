package user.service

import user.entity.PermissionEntity
import user.mapper.PermissionMapper
import user.models.{ Permission, PermissionId }
import user.repository.PermissionRepository
import zio.*

import java.util.UUID
import java.time.Instant

class PermissionServiceImpl(permissionRepository: PermissionRepository) extends PermissionService:
  override def findPermissionById(id: PermissionId): Task[Permission] =
    permissionRepository.findById(id.value).flatMap(PermissionMapper.toModelFromEntity)

  override def findPermissionByName(name: String): Task[Permission] =
    permissionRepository.findByName(name).flatMap(PermissionMapper.toModelFromEntity)

  override def createPermission(name: String, description: Option[String]): Task[Permission] =
    for
      now <- ZIO.succeed(Instant.now())
      permissionEntity =
        PermissionEntity(
          id = UUID.randomUUID(),
          name = name,
          description = description,
          createdAt = now,
          updatedAt = now,
        )
      createdEntity <- permissionRepository.create(permissionEntity)
      permission <- PermissionMapper.toModelFromEntity(createdEntity)
    yield permission

  override def updatePermission(
    id: PermissionId,
    name: String,
    description: Option[String],
  ): Task[Permission] =
    for
      existingEntity <- permissionRepository.findById(id.value)
      now = Instant.now()
      updatedEntity =
        existingEntity.copy(
          name = name,
          description = description,
          updatedAt = now,
        )
      resultEntity <- permissionRepository.update(updatedEntity)
      permission <- PermissionMapper.toModelFromEntity(resultEntity)
    yield permission

  override def deletePermission(id: PermissionId): Task[Unit] =
    permissionRepository.delete(id.value)

  override def findAllPermissions(): Task[List[Permission]] =
    for
      permissionEntities <- permissionRepository.findAll()
      permissions <- ZIO.foreach(permissionEntities)(PermissionMapper.toModelFromEntity)
    yield permissions

object PermissionServiceImpl:
  val layer: URLayer[PermissionRepository, PermissionService] =
    ZLayer.fromFunction(PermissionServiceImpl(_))
