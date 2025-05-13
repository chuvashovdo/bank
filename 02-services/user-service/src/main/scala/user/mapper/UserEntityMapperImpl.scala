package user.mapper

import java.time.Instant
import user.entity.UserEntity
import user.models.{ User, UserId, Email, FirstName, LastName }
import zio.*
import common.errors.{ CorruptedDataInDBError, ValidationError }

class UserEntityMapperImpl extends UserEntityMapper:
  override def fromUser(
    user: User,
    createdAt: Instant,
    updatedAt: Instant,
  ): Task[UserEntity] =
    ZIO.succeed:
      UserEntity(
        id = user.id.value,
        email = user.email.value,
        passwordHash = user.passwordHash,
        firstName = user.firstName.map(_.value),
        lastName = user.lastName.map(_.value),
        isActive = user.isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )

  override def toUser(entity: UserEntity): Task[User] =
    for
      validatedUserId <-
        ZIO
          .fromEither(UserId(entity.id))
          .mapError:
            case validationError: ValidationError =>
              CorruptedDataInDBError(
                entityId = entity.id,
                fieldName = "id",
                fieldValue = entity.id,
                validationErrorMessage = validationError.developerFriendlyMessage,
              )

      validatedEmail <-
        ZIO
          .fromEither(Email(entity.email))
          .mapError { validationError =>
            CorruptedDataInDBError(
              entityId = entity.id,
              fieldName = "email",
              fieldValue = entity.email,
              validationErrorMessage = validationError.developerFriendlyMessage,
            )
          }

      validatedFirstName <-
        ZIO.foreach(entity.firstName) { nameStr =>
          ZIO
            .fromEither(FirstName(nameStr))
            .mapError { validationError =>
              CorruptedDataInDBError(
                entityId = entity.id,
                fieldName = "firstName",
                fieldValue = nameStr,
                validationErrorMessage = validationError.developerFriendlyMessage,
              )
            }
        }

      validatedLastName <-
        ZIO.foreach(entity.lastName) { nameStr =>
          ZIO
            .fromEither(LastName(nameStr))
            .mapError { validationError =>
              CorruptedDataInDBError(
                entityId = entity.id,
                fieldName = "lastName",
                fieldValue = nameStr,
                validationErrorMessage = validationError.developerFriendlyMessage,
              )
            }
        }
    yield User(
      id = validatedUserId,
      email = validatedEmail,
      passwordHash = entity.passwordHash,
      firstName = validatedFirstName,
      lastName = validatedLastName,
      isActive = entity.isActive,
    )

  override def createUserEntity(
    id: UserId,
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[UserEntity] =
    ZIO.succeed(
      UserEntity(
        id.value,
        email,
        passwordHash,
        firstName,
        lastName,
        true,
        Instant.now(),
        Instant.now(),
      )
    )

object UserEntityMapperImpl:
  val layer: ULayer[UserEntityMapper] =
    ZLayer.succeed(new UserEntityMapperImpl())
