package user.service

import user.models.{ User, UserId, Email, Password, FirstName, LastName, Role, RoleId }
import zio.*

trait UserService:
  def findUserById(id: UserId): Task[User]
  def findUserByEmail(email: Email): Task[User]
  def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User]
  def validateCredentials(email: Email, password: Password): Task[User]
  def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User]
  def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Unit]
  def deactivateUser(id: UserId): Task[Unit]
  def findAllUsers(): Task[List[User]]

  def assignRoleToUser(userId: UserId, roleId: RoleId): Task[Unit]
  def revokeRoleFromUser(userId: UserId, roleId: RoleId): Task[Unit]
  def getUserRoles(userId: UserId): Task[Set[Role]]
  def updateUserRoles(userId: UserId, newRoleIds: Set[RoleId]): Task[Unit]

object UserService:
  def findUserById(id: UserId): RIO[UserService, User] =
    ZIO.serviceWithZIO[UserService](_.findUserById(id))
  def findUserByEmail(email: Email): RIO[UserService, User] =
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
  ): RIO[UserService, User] =
    ZIO.serviceWithZIO[UserService](_.validateCredentials(email, password))
  def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): RIO[UserService, User] =
    ZIO.serviceWithZIO[UserService](_.updateUser(id, firstName, lastName))
  def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): RIO[UserService, Unit] =
    ZIO.serviceWithZIO[UserService](_.changePassword(id, oldPassword, newPassword))
  def deactivateUser(id: UserId): RIO[UserService, Unit] =
    ZIO.serviceWithZIO[UserService](_.deactivateUser(id))
  def findAllUsers(): RIO[UserService, List[User]] =
    ZIO.serviceWithZIO[UserService](_.findAllUsers())

  def assignRoleToUser(userId: UserId, roleId: RoleId): RIO[UserService, Unit] =
    ZIO.serviceWithZIO[UserService](_.assignRoleToUser(userId, roleId))
  def revokeRoleFromUser(userId: UserId, roleId: RoleId): RIO[UserService, Unit] =
    ZIO.serviceWithZIO[UserService](_.revokeRoleFromUser(userId, roleId))
  def getUserRoles(userId: UserId): RIO[UserService, Set[Role]] =
    ZIO.serviceWithZIO[UserService](_.getUserRoles(userId))
  def updateUserRoles(userId: UserId, newRoleIds: Set[RoleId]): RIO[UserService, Unit] =
    ZIO.serviceWithZIO[UserService](_.updateUserRoles(userId, newRoleIds))
