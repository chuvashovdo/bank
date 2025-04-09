package user.mapper

import java.time.Instant
import user.entity.UserEntity
import user.models.User
import zio.*
import user.models.UserId
class UserEntityMapperImpl extends UserEntityMapper:
  override def fromUser(
    user: User,
    createdAt: Instant,
    updatedAt: Instant,
  ): Task[UserEntity] =
    ZIO.succeed(
      UserEntity(
        user.id,
        user.email,
        user.passwordHash,
        user.firstName,
        user.lastName,
        true,
        createdAt,
        updatedAt,
      )
    )
  override def toUser(entity: UserEntity): Task[User] =
    ZIO.succeed:
      User(
        entity.id,
        entity.email,
        entity.passwordHash,
        entity.firstName,
        entity.lastName,
      )

  override def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): Task[UserEntity] =
    ZIO.succeed(
      UserEntity(
        id,
        email,
        passwordHash,
        firstName,
        lastName,
        true,
        Instant.now(),
        Instant.now(),
      )
    )
