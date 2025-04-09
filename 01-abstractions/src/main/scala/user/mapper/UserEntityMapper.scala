package user.mapper

import zio.*
import user.entity.UserEntity
import user.models.User
import java.time.Instant
import user.models.UserId

trait UserEntityMapper:
  def toUser(entity: UserEntity): ZIO[UserEntityMapper, Nothing, User]
  def toUserEntity(user: User): ZIO[UserEntityMapper, Nothing, UserEntity]
  def fromUser(
    user: User,
    createdAt: Instant,
    updatedAt: Instant,
  ): ZIO[UserEntityMapper, Nothing, UserEntity]
  def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): ZIO[UserEntityMapper, Nothing, UserEntity]

object UserEntityMapper:
  def toUser(entity: UserEntity): ZIO[UserEntityMapper, Nothing, User] =
    ZIO.serviceWithZIO[UserEntityMapper](_.toUser(entity))
  def toUserEntity(user: User): ZIO[UserEntityMapper, Nothing, UserEntity] =
    ZIO.serviceWithZIO[UserEntityMapper](_.toUserEntity(user))
  def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): ZIO[UserEntityMapper, Nothing, UserEntity] =
    ZIO.serviceWithZIO[UserEntityMapper](
      _.createUserEntity(id, email, passwordHash, firstName, lastName)
    )
