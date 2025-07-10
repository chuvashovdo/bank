package user.mapper

import zio.{ Task, ZIO }
import user.entity.UserEntity
import user.models.*
import java.time.Instant
import user.models.dto.{ UserResponse, RoleResponse, PermissionResponse }

object UserMapper:
  def toModelFromEntity(entity: UserEntity, roles: Set[Role]): Task[User] =
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
      roles = roles,
    )

  def toEntityFromModel(model: User): UserEntity =
    UserEntity(
      id = model.id.value,
      email = model.email.value,
      passwordHash = model.passwordHash,
      firstName = model.firstName.map(_.value),
      lastName = model.lastName.map(_.value),
      isActive = model.isActive,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
    )

  def toResponseFromModel(model: User): UserResponse =
    UserResponse(
      id = model.id,
      email = model.email,
      firstName = model.firstName,
      lastName = model.lastName,
      roles =
        model
          .roles
          .map { role =>
            RoleResponse(
              role.id,
              role.name,
              role.description,
              role.permissions.map(p => PermissionResponse(p.id, p.name, p.description)).toList,
            )
          }
          .toList,
    )
