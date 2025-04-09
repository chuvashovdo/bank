package user.service

import user.models.User
import zio.*

trait UserService:
  def findUserById(id: UserId): Task[Option[User]]
  def findUserByEmail(email: String): Task[Option[User]]
  def registerUser(
    email: String,
    password: String,
    firstName: String,
    lastName: String,
  ): Task[User]
  def validateCredentials(email: String, password: String): Task[Option[User]]
  def updateUser(
    id: UserId,
    firstName: String,
    lastName: String,
  ): Task[User]
  def changePassword(
    id: UserId,
    oldPassword: String,
    newPassword: String,
  ): Task[Boolean]
  def deactivateUser(id: UserId): Task[Boolean]

object UserService:
  def findUserById(id: UserId): ZIO[UserService, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserById(id))
  def findUserByEmail(email: String): ZIO[UserService, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.findUserByEmail(email))
  def registerUser(
    email: String,
    password: String,
    firstName: String,
    lastName: String,
  ): ZIO[UserService, Throwable, User] =
    ZIO.serviceWithZIO[UserService](_.registerUser(email, password, firstName, lastName))
  def validateCredentials(
    email: String,
    password: String,
  ): ZIO[UserService, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserService](_.validateCredentials(email, password))
  def updateUser(
    id: UserId,
    firstName: String,
    lastName: String,
  ): ZIO[UserService, Throwable, User] =
    ZIO.serviceWithZIO[UserService](_.updateUser(id, firstName, lastName))
  def changePassword(
    id: UserId,
    oldPassword: String,
    newPassword: String,
  ): ZIO[UserService, Throwable, Boolean] =
    ZIO.serviceWithZIO[UserService](_.changePassword(id, oldPassword, newPassword))
  def deactivateUser(id: UserId): ZIO[UserService, Throwable, Boolean] =
    ZIO.serviceWithZIO[UserService](_.deactivateUser(id))
