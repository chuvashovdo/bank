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

final case class PermissionNotFoundError(identifier: UUID | String) extends UserError:
  override val errorCode: String =
    "PERMISSION_NOT_FOUND"
  override def message: String =
    s"Permission with identifier '$identifier' not found."

final case class RoleNotFoundError(identifier: UUID | String) extends UserError:
  override val errorCode: String =
    "ROLE_NOT_FOUND"
  override def message: String =
    s"Role with identifier '$identifier' not found."
