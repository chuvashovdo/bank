package user.service

import user.entity.RoleEntity
import user.mapper.{ PermissionMapper, RoleMapper }
import user.models.{ Permission, PermissionId, Role, RoleId }
import user.repository.{ PermissionRepository, RoleRepository }
import zio.*

import java.util.UUID
import java.time.Instant

class RoleServiceImpl(
  roleRepository: RoleRepository,
  permissionRepository: PermissionRepository,
) extends RoleService:
  private def loadRoleWithPermissions(roleEntity: RoleEntity): Task[Role] =
    for
      permissionIds <- roleRepository.findPermissionsByRoleId(roleEntity.id)
      permissions <-
        ZIO
          .foreach(permissionIds) { permissionId =>
            for
              permissionEntity <- permissionRepository.findById(permissionId)
              permission <- PermissionMapper.toModelFromEntity(permissionEntity)
            yield permission
          }
          .map(_.toSet)
      role <- RoleMapper.toModelFromEntity(roleEntity, permissions)
    yield role

  override def findRoleById(id: RoleId): Task[Role] =
    roleRepository.findById(id.value).flatMap(loadRoleWithPermissions)

  override def findRoleByName(name: String): Task[Role] =
    roleRepository.findByName(name).flatMap(loadRoleWithPermissions)

  override def createRole(name: String, description: Option[String]): Task[Role] =
    for
      now <- ZIO.succeed(Instant.now())
      roleEntity =
        RoleEntity(
          id = UUID.randomUUID(),
          name = name,
          description = description,
          createdAt = now,
          updatedAt = now,
        )
      createdEntity <- roleRepository.create(roleEntity)
      role <- loadRoleWithPermissions(createdEntity)
    yield role

  override def updateRole(
    id: RoleId,
    name: String,
    description: Option[String],
  ): Task[Role] =
    for
      existingEntity <- roleRepository.findById(id.value)
      now <- ZIO.succeed(Instant.now())
      updatedEntity =
        existingEntity.copy(
          name = name,
          description = description,
          updatedAt = now,
        )
      resultEntity <- roleRepository.update(updatedEntity)
      role <- loadRoleWithPermissions(resultEntity)
    yield role

  override def deleteRole(id: RoleId): Task[Unit] =
    roleRepository.delete(id.value)

  override def findAllRoles(): Task[List[Role]] =
    for
      roleEntities <- roleRepository.findAll()
      roles <- ZIO.foreach(roleEntities)(loadRoleWithPermissions)
    yield roles

  override def addPermissionToRole(roleId: RoleId, permissionId: PermissionId): Task[Unit] =
    roleRepository.addPermissionToRole(roleId.value, permissionId.value)

  override def removePermissionFromRole(roleId: RoleId, permissionId: PermissionId): Task[Unit] =
    roleRepository.removePermissionFromRole(roleId.value, permissionId.value)

  override def findPermissionsForRole(roleId: RoleId): Task[Set[Permission]] =
    for
      permissionIds <- roleRepository.findPermissionsByRoleId(roleId.value)
      permissions <-
        ZIO
          .foreach(permissionIds) { permissionId =>
            for
              permissionEntity <- permissionRepository.findById(permissionId)
              permission <- PermissionMapper.toModelFromEntity(permissionEntity)
            yield permission
          }
          .map(_.toSet)
    yield permissions

object RoleServiceImpl:
  val layer: URLayer[RoleRepository & PermissionRepository, RoleService] =
    ZLayer:
      for
        roleRepository <- ZIO.service[RoleRepository]
        permissionRepository <- ZIO.service[PermissionRepository]
      yield RoleServiceImpl(roleRepository, permissionRepository)
