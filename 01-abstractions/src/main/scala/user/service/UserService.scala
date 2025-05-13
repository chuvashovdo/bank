package user.service

import user.models.{ User, UserId, Email, Password, FirstName, LastName }
import zio.*

trait UserService:
  def findUserById(id: UserId): Task[Option[User]]
  def findUserByEmail(email: Email): Task[Option[User]]
  def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User]
  def validateCredentials(email: Email, password: Password): Task[Option[User]]
  def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[Option[User]]
  def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Boolean]
  def deactivateUser(id: UserId): Task[Boolean]

object UserService:
  def findUserById(id: UserId): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserById(id))
  def findUserByEmail(email: Email): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserByEmail(email))
  def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): RIO[UserService, User] =
    ZIO.serviceWithZIO[UserService](_.registerUser(email, password, firstName, lastName))
  def validateCredentials(
    email: Email,
    password: Password,
  ): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.validateCredentials(email, password))
  def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.updateUser(id, firstName, lastName))
  def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): RIO[UserService, Boolean] =
    ZIO.serviceWithZIO[UserService](_.changePassword(id, oldPassword, newPassword))
  def deactivateUser(id: UserId): RIO[UserService, Boolean] =
    ZIO.serviceWithZIO[UserService](_.deactivateUser(id))
