package user.service

import user.models.{ Role, RoleId, Permission, PermissionId }
import zio.*

trait RoleService:
  def findRoleById(id: RoleId): Task[Role]
  def findRoleByName(name: String): Task[Role]
  def createRole(name: String, description: Option[String]): Task[Role]
  def updateRole(
    id: RoleId,
    name: String,
    description: Option[String],
  ): Task[Role]
  def deleteRole(id: RoleId): Task[Unit]
  def findAllRoles(): Task[List[Role]]

  def addPermissionToRole(roleId: RoleId, permissionId: PermissionId): Task[Unit]
  def removePermissionFromRole(roleId: RoleId, permissionId: PermissionId): Task[Unit]
  def findPermissionsForRole(roleId: RoleId): Task[Set[Permission]]

object RoleService:
  def findRoleById(id: RoleId): RIO[RoleService, Role] =
    ZIO.serviceWithZIO[RoleService](_.findRoleById(id))
  def findRoleByName(name: String): RIO[RoleService, Role] =
    ZIO.serviceWithZIO[RoleService](_.findRoleByName(name))
  def createRole(name: String, description: Option[String]): RIO[RoleService, Role] =
    ZIO.serviceWithZIO[RoleService](_.createRole(name, description))
  def updateRole(
    id: RoleId,
    name: String,
    description: Option[String],
  ): RIO[RoleService, Role] =
    ZIO.serviceWithZIO[RoleService](_.updateRole(id, name, description))
  def deleteRole(id: RoleId): RIO[RoleService, Unit] =
    ZIO.serviceWithZIO[RoleService](_.deleteRole(id))
  def findAllRoles(): RIO[RoleService, List[Role]] =
    ZIO.serviceWithZIO[RoleService](_.findAllRoles())

  def addPermissionToRole(roleId: RoleId, permissionId: PermissionId): RIO[RoleService, Unit] =
    ZIO.serviceWithZIO[RoleService](_.addPermissionToRole(roleId, permissionId))
  def removePermissionFromRole(roleId: RoleId, permissionId: PermissionId): RIO[RoleService, Unit] =
    ZIO.serviceWithZIO[RoleService](_.removePermissionFromRole(roleId, permissionId))
  def findPermissionsForRole(roleId: RoleId): RIO[RoleService, Set[Permission]] =
    ZIO.serviceWithZIO[RoleService](_.findPermissionsForRole(roleId))
