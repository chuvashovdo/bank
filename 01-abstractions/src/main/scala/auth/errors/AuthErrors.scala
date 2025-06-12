package auth.errors

import common.errors.BusinessError

sealed trait AuthError extends BusinessError

final case class RefreshTokenNotFoundError(token: String) extends AuthError:
  override val errorCode: String =
    "REFRESH_TOKEN_NOT_FOUND"
  override def message: String =
    s"Refresh token '$token' not found or has expired."
