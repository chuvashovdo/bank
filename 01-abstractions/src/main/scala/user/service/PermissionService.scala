package user.service

import user.models.{ Permission, PermissionId }
import zio.*

trait PermissionService:
  def findPermissionById(id: PermissionId): Task[Permission]
  def findPermissionByName(name: String): Task[Permission]
  def createPermission(name: String, description: Option[String]): Task[Permission]
  def updatePermission(
    id: PermissionId,
    name: String,
    description: Option[String],
  ): Task[Permission]
  def deletePermission(id: PermissionId): Task[Unit]
  def findAllPermissions(): Task[List[Permission]]

object PermissionService:
  def findPermissionById(id: PermissionId): RIO[PermissionService, Permission] =
    ZIO.serviceWithZIO[PermissionService](_.findPermissionById(id))
  def findPermissionByName(name: String): RIO[PermissionService, Permission] =
    ZIO.serviceWithZIO[PermissionService](_.findPermissionByName(name))
  def createPermission(
    name: String,
    description: Option[String],
  ): RIO[PermissionService, Permission] =
    ZIO.serviceWithZIO[PermissionService](_.createPermission(name, description))
  def updatePermission(
    id: PermissionId,
    name: String,
    description: Option[String],
  ): RIO[PermissionService, Permission] =
    ZIO.serviceWithZIO[PermissionService](_.updatePermission(id, name, description))
  def deletePermission(id: PermissionId): RIO[PermissionService, Unit] =
    ZIO.serviceWithZIO[PermissionService](_.deletePermission(id))
  def findAllPermissions(): RIO[PermissionService, List[Permission]] =
    ZIO.serviceWithZIO[PermissionService](_.findAllPermissions())
