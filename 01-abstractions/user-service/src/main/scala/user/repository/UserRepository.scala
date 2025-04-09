package user.repository

import user.models.User
import zio.*

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
  def findById(id: UserId): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): ZIO[UserRepository, Throwable, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(email, passwordHash, firstName, lastName))
  def update(
    id: UserId,
    firstName: String,
    lastName: String,
  ): ZIO[UserRepository, Throwable, User] =
    ZIO.serviceWithZIO[UserRepository](_.update(id, firstName, lastName))
  def updatePassword(id: UserId, passwordHash: String): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: UserId): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
