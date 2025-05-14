package user.repository

import user.models.User
import zio.*
trait UserRepository:
  def findById(id: String): Task[User]
  def findByEmail(email: String): Task[User]
  def create(
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def update(
    id: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def updatePassword(id: String, passwordHash: String): Task[Unit]
  def deactivate(id: String): Task[Unit]

object UserRepository:
  def findById(id: String): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(email, passwordHash, firstName, lastName))
  def update(
    id: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.update(id, firstName, lastName))
  def updatePassword(id: String, passwordHash: String): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: String): RIO[UserRepository, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
