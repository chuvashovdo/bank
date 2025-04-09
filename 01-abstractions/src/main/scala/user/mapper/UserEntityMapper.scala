package user.mapper

import zio.*
import user.entity.UserEntity
import user.models.User
import java.time.Instant
import user.models.UserId

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

object UserEntityMapper:
  def toUser(entity: UserEntity): RIO[UserEntityMapper, User] =
    ZIO.serviceWithZIO[UserEntityMapper](_.toUser(entity))
  def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): RIO[UserEntityMapper, UserEntity] =
    ZIO.serviceWithZIO[UserEntityMapper](
      _.createUserEntity(id, email, passwordHash, firstName, lastName)
    )
