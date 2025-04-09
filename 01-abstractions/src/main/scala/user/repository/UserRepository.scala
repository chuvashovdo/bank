package user.repository

import user.models.User
import zio.*
import user.models.UserId
trait UserRepository:
  def findById(id: UserId): Task[Option[User]]
  def findByEmail(email: String): Task[Option[User]]
  def create(
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): Task[User]
  def update(
    id: UserId,
    firstName: String,
    lastName: String,
  ): Task[User]
  def updatePassword(id: UserId, passwordHash: String): Task[Unit]
  def deactivate(id: UserId): Task[Unit]

object UserRepository:
  def findById(id: UserId): RIO[UserRepository, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): RIO[UserRepository, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(email, passwordHash, firstName, lastName))
  def update(
    id: UserId,
    firstName: String,
    lastName: String,
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.update(id, firstName, lastName))
  def updatePassword(id: UserId, passwordHash: String): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: UserId): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
