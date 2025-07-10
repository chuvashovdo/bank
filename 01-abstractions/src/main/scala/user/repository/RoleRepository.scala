package user.repository

import user.entity.RoleEntity
import zio.*
import java.util.UUID

trait RoleRepository:
  def findById(id: UUID): Task[RoleEntity]
  def findByName(name: String): Task[RoleEntity]
  def create(role: RoleEntity): Task[RoleEntity]
  def update(role: RoleEntity): Task[RoleEntity]
  def delete(id: UUID): Task[Unit]
  def findAll(): Task[List[RoleEntity]]

  def addPermissionToRole(roleId: UUID, permissionId: UUID): Task[Unit]
  def removePermissionFromRole(roleId: UUID, permissionId: UUID): Task[Unit]
  def findPermissionsByRoleId(roleId: UUID): Task[List[UUID]]

object RoleRepository:
  def findById(id: UUID): RIO[RoleRepository, RoleEntity] =
    ZIO.serviceWithZIO[RoleRepository](_.findById(id))
  def findByName(name: String): RIO[RoleRepository, RoleEntity] =
    ZIO.serviceWithZIO[RoleRepository](_.findByName(name))
  def create(role: RoleEntity): RIO[RoleRepository, RoleEntity] =
    ZIO.serviceWithZIO[RoleRepository](_.create(role))
  def update(role: RoleEntity): RIO[RoleRepository, RoleEntity] =
    ZIO.serviceWithZIO[RoleRepository](_.update(role))
  def delete(id: UUID): RIO[RoleRepository, Unit] =
    ZIO.serviceWithZIO[RoleRepository](_.delete(id))
  def findAll(): RIO[RoleRepository, List[RoleEntity]] =
    ZIO.serviceWithZIO[RoleRepository](_.findAll())

  def addPermissionToRole(roleId: UUID, permissionId: UUID): RIO[RoleRepository, Unit] =
    ZIO.serviceWithZIO[RoleRepository](_.addPermissionToRole(roleId, permissionId))
  def removePermissionFromRole(roleId: UUID, permissionId: UUID): RIO[RoleRepository, Unit] =
    ZIO.serviceWithZIO[RoleRepository](_.removePermissionFromRole(roleId, permissionId))
  def findPermissionsByRoleId(roleId: UUID): RIO[RoleRepository, List[UUID]] =
    ZIO.serviceWithZIO[RoleRepository](_.findPermissionsByRoleId(roleId))
