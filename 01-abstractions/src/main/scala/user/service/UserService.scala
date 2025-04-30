package user.service

import user.models.User
import user.models.UserId
import zio.*

trait UserService:
  def findUserById(id: UserId): Task[Option[User]]
  def findUserByEmail(email: String): Task[Option[User]]
  def registerUser(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User]
  def validateCredentials(email: String, password: String): Task[Option[User]]
  def updateUser(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[Option[User]]
  def changePassword(
    id: UserId,
    oldPassword: String,
    newPassword: String,
  ): Task[Boolean]
  def deactivateUser(id: UserId): Task[Boolean]

object UserService:
  def findUserById(id: UserId): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserById(id))
  def findUserByEmail(email: String): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserByEmail(email))
  def registerUser(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserService, User] =
    ZIO.serviceWithZIO[UserService](_.registerUser(email, password, firstName, lastName))
  def validateCredentials(
    email: String,
    password: String,
  ): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.validateCredentials(email, password))
  def updateUser(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[UserService, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.updateUser(id, firstName, lastName))
  def changePassword(
    id: UserId,
    oldPassword: String,
    newPassword: String,
  ): RIO[UserService, Boolean] =
    ZIO.serviceWithZIO[UserService](_.changePassword(id, oldPassword, newPassword))
  def deactivateUser(id: UserId): RIO[UserService, Boolean] =
    ZIO.serviceWithZIO[UserService](_.deactivateUser(id))
