package user.repository

import user.entity.UserEntity
import zio.*
import java.util.UUID

trait UserRepository:
  def findById(id: UUID): Task[UserEntity]
  def findByEmail(email: String): Task[UserEntity]
  def create(user: UserEntity): Task[UserEntity]
  def update(user: UserEntity): Task[UserEntity]
  def updatePassword(id: UUID, passwordHash: String): Task[Unit]
  def deactivate(id: UUID): Task[Unit]

object UserRepository:
  def findById(id: UUID): RIO[UserRepository, UserEntity] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): RIO[UserRepository, UserEntity] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(user: UserEntity): RIO[UserRepository, UserEntity] =
    ZIO.serviceWithZIO[UserRepository](_.create(user))
  def update(user: UserEntity): RIO[UserRepository, UserEntity] =
    ZIO.serviceWithZIO[UserRepository](_.update(user))
  def updatePassword(id: UUID, passwordHash: String): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: UUID): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
