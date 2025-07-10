package user.repository

import user.entity.PermissionEntity
import zio.*
import java.util.UUID

trait PermissionRepository:
  def findById(id: UUID): Task[PermissionEntity]
  def findByName(name: String): Task[PermissionEntity]
  def create(permission: PermissionEntity): Task[PermissionEntity]
  def update(permission: PermissionEntity): Task[PermissionEntity]
  def delete(id: UUID): Task[Unit]
  def findAll(): Task[List[PermissionEntity]]

object PermissionRepository:
  def findById(id: UUID): RIO[PermissionRepository, PermissionEntity] =
    ZIO.serviceWithZIO[PermissionRepository](_.findById(id))
  def findByName(name: String): RIO[PermissionRepository, PermissionEntity] =
    ZIO.serviceWithZIO[PermissionRepository](_.findByName(name))
  def create(permission: PermissionEntity): RIO[PermissionRepository, PermissionEntity] =
    ZIO.serviceWithZIO[PermissionRepository](_.create(permission))
  def update(permission: PermissionEntity): RIO[PermissionRepository, PermissionEntity] =
    ZIO.serviceWithZIO[PermissionRepository](_.update(permission))
  def delete(id: UUID): RIO[PermissionRepository, Unit] =
    ZIO.serviceWithZIO[PermissionRepository](_.delete(id))
  def findAll(): RIO[PermissionRepository, List[PermissionEntity]] =
    ZIO.serviceWithZIO[PermissionRepository](_.findAll())
