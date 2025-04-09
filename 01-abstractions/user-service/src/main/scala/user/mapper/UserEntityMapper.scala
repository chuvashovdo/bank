package user.mapper

import zio.*
import user.entity.UserEntity
import user.models.User

trait UserEntityMapper:
  def toUser(entity: UserEntity): Task[User]
  def fromUser(
    user: User,
    createdAt: Instant,
    updatedAt: Instant,
  ): Task[UserEntity]
  def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): Task[UserEntity]

object UserMapper:
  def toUser(entity: UserEntity): ZIO[UserMapper, Nothing, User] =
    ZIO.serviceWithZIO[UserMapper](_.toUser(entity))
  def toUserEntity(user: User): ZIO[UserMapper, Nothing, UserEntity] =
    ZIO.serviceWithZIO[UserMapper](_.toUserEntity(user))
  def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): ZIO[UserMapper, Nothing, UserEntity] =
    ZIO.serviceWithZIO[UserMapper](_.createUserEntity(id, email, passwordHash, firstName, lastName))
