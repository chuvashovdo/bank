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

final case class RefreshTokenNotFoundError(token: String) extends BusinessError:
  override val errorCode: String =
    "REFRESH_TOKEN_NOT_FOUND"
  override def message: String =
    s"Refresh token '$token' not found or has expired."

final case class TokenMissingClaimError(claimName: String, tokenType: String = "Access")
    extends BusinessError:
  override val errorCode: String =
    "TOKEN_MISSING_CLAIM"
  override def message: String =
    s"$tokenType token is missing required claim: '$claimName'."

final case class TokenExpiredError(
  tokenType: String = "Access",
  expiredAt: Option[Long] = None,
  now: Option[Long] = None,
) extends BusinessError:
  override val errorCode: String =
    "TOKEN_EXPIRED"
  override def message: String =
    val details =
      (expiredAt, now) match
        case (Some(exp), Some(n)) => s" (expired at: $exp, current time: $n)"
        case _ => ""
    s"$tokenType token has expired$details."

final case class InvalidTokenSubjectFormatError(
  subject: String,
  reason: String,
  tokenType: String = "Access",
) extends BusinessError:
  override val errorCode: String =
    "INVALID_TOKEN_SUBJECT_FORMAT"
  override def message: String =
    s"Invalid subject format in $tokenType token. Subject: '$subject', Reason: $reason."

final case class TokenDecodingError(tokenType: String = "Access", details: Option[String] = None)
    extends BusinessError:
  override val errorCode: String =
    "TOKEN_DECODING_ERROR"
  override def message: String =
    val detailMessage = details.map(d => s": $d").getOrElse("")
    s"$tokenType token is malformed or could not be decoded$detailMessage."
