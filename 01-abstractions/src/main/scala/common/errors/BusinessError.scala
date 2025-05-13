package common.errors

import user.models.{ Email, UserId }

trait BusinessError extends Throwable:
  def errorCode: String
  def message: String

final case class UserAlreadyExistsError(email: Email) extends BusinessError:
  override val errorCode: String =
    "USER_ALREADY_EXISTS"
  override def message: String =
    s"User with email ${email.value} already exists."

final case class InvalidCredentialsError() extends BusinessError:
  override val errorCode: String =
    "INVALID_CREDENTIALS"
  override def message: String =
    "Invalid email or password."

final case class UserNotActiveError(identifier: String) extends BusinessError:
  override val errorCode: String =
    "USER_NOT_ACTIVE"
  override def message: String =
    s"User $identifier is not active."

final case class InvalidOldPasswordError(userId: UserId) extends BusinessError:
  override val errorCode: String =
    "INVALID_OLD_PASSWORD"
  override def message: String =
    s"Invalid old password for user ${userId.value}."

final case class UserNotFoundError(identifier: String) extends BusinessError:
  override val errorCode: String =
    "USER_NOT_FOUND"
  override def message: String =
    s"User with identifier '$identifier' not found."

final case class CorruptedDataInDBError(
  entityId: String,
  fieldName: String,
  fieldValue: String,
  validationErrorMessage: String,
) extends BusinessError:
  override val errorCode: String =
    "CORRUPTED_DATA_IN_DB"
  override def message: String =
    s"Corrupted data in DB for entity $entityId, field '$fieldName' (value: '$fieldValue'). Validation failed: $validationErrorMessage"
