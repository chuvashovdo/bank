package user.mapper

import zio.{ Task, ZIO }
import user.entity.UserEntity
import user.models.*

object UserMapper:
  def toModel(entity: UserEntity): Task[User] =
    for
      email <- ZIO.fromEither(Email(entity.email)).mapError(e => new Exception(e.toString))
      firstName <-
        ZIO.foreach(entity.firstName)(fn =>
          ZIO.fromEither(FirstName(fn)).mapError(e => new Exception(e.toString))
        )
      lastName <-
        ZIO.foreach(entity.lastName)(ln =>
          ZIO.fromEither(LastName(ln)).mapError(e => new Exception(e.toString))
        )
    yield User(
      id = UserId(entity.id),
      email = email,
      passwordHash = entity.passwordHash,
      firstName = firstName,
      lastName = lastName,
      isActive = entity.isActive,
    )

  def toEntity(model: User): UserEntity =
    UserEntity(
      id = model.id.value,
      email = model.email.value,
      passwordHash = model.passwordHash,
      firstName = model.firstName.map(_.value),
      lastName = model.lastName.map(_.value),
      isActive = model.isActive,
      createdAt = java.time.Instant.now(),
      updatedAt = java.time.Instant.now(),
    )
