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
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def update(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[Option[User]]
  def updatePassword(id: UserId, passwordHash: String): Task[Boolean]
  def deactivate(id: UserId): Task[Boolean]

object UserRepository:
  def findById(id: UserId): RIO[UserRepository, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findById(id))
  def findByEmail(email: String): RIO[UserRepository, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findByEmail(email))
  def create(
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(email, passwordHash, firstName, lastName))
  def update(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserRepository, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.update(id, firstName, lastName))
  def updatePassword(id: UserId, passwordHash: String): RIO[UserRepository, Boolean] =
    ZIO.serviceWithZIO[UserRepository](_.updatePassword(id, passwordHash))
  def deactivate(id: UserId): RIO[UserRepository, Boolean] =
    ZIO.serviceWithZIO[UserRepository](_.deactivate(id))
