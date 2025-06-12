package user.errors

import common.errors.BusinessError
import java.util.UUID

sealed trait UserError extends BusinessError

final case class UserNotFoundError(identifier: UUID | String) extends UserError:
  override val errorCode: String =
    "USER_NOT_FOUND"
  override def message: String =
    s"User with identifier '$identifier' not found."

final case class UserAlreadyExistsError(email: String) extends UserError:
  override val errorCode: String =
    "USER_ALREADY_EXISTS"
  override def message: String =
    s"User with email $email already exists."

final case class UserNotActiveError(userId: UUID) extends UserError:
  override val errorCode: String =
    "USER_NOT_ACTIVE"
  override def message: String =
    s"User $userId is not active."

final case class InvalidEmailError(details: String) extends UserError:
  override val errorCode: String =
    "INVALID_EMAIL"
  override def message: String =
    s"Invalid email provided. Details: $details"

final case class InvalidFirstNameError(details: String) extends UserError:
  override val errorCode: String =
    "INVALID_FIRST_NAME"
  override def message: String =
    s"Invalid first name provided. Details: $details"

final case class InvalidLastNameError(details: String) extends UserError:
  override val errorCode: String =
    "INVALID_LAST_NAME"
  override def message: String =
    s"Invalid last name provided. Details: $details"

final case class InvalidCredentialsError() extends UserError:
  override val errorCode: String =
    "INVALID_CREDENTIALS"
  override def message: String =
    "Invalid email or password."

final case class InvalidOldPasswordError(userId: UUID) extends UserError:
  override val errorCode: String =
    "INVALID_OLD_PASSWORD"
  override def message: String =
    s"Invalid old password for user $userId."
