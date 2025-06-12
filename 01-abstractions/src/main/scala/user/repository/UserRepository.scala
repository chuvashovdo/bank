package user.repository

import user.models.User
import zio.*
import java.util.UUID

trait UserRepository:
  def findById(id: UUID): Task[User]
  def findByEmail(email: String): Task[User]
  def create(
    id: UUID,
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def update(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def updatePassword(id: UUID, passwordHash: String): Task[Unit]
  def deactivate(id: UUID): Task[Unit]

object UserRepository:
  def findById(id: UUID): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(
    id: UUID,
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(id, email, passwordHash, firstName, lastName))
  def update(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.update(id, firstName, lastName))
  def updatePassword(id: UUID, passwordHash: String): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: UUID): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
