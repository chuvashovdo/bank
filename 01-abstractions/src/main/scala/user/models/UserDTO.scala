package user.models

import zio.json.*

final case class RegisterUserRequest(
  email: String,
  password: String,
  firstName: Option[String],
  lastName: Option[String],
)

object RegisterUserRequest:
  given JsonCodec[RegisterUserRequest] =
    DeriveJsonCodec.gen[RegisterUserRequest]

final case class LoginRequest(
  email: String,
  password: String,
)

object LoginRequest:
  given JsonCodec[LoginRequest] =
    DeriveJsonCodec.gen[LoginRequest]

final case class UpdateUserRequest(
  firstName: Option[String],
  lastName: Option[String],
)

object UpdateUserRequest:
  given JsonCodec[UpdateUserRequest] =
    DeriveJsonCodec.gen[UpdateUserRequest]

final case class ChangePasswordRequest(
  oldPassword: String,
  newPassword: String,
)

object ChangePasswordRequest:
  given JsonCodec[ChangePasswordRequest] =
    DeriveJsonCodec.gen[ChangePasswordRequest]

final case class UserResponse(
  id: String,
  email: String,
  firstName: Option[String],
  lastName: Option[String],
)

object UserResponse:
  given JsonCodec[UserResponse] =
    DeriveJsonCodec.gen[UserResponse]

final case class AuthResponse(
  accessToken: String,
  refreshToken: String,
  expiresAt: Long,
  user: UserResponse,
)

object AuthResponse:
  given JsonCodec[AuthResponse] =
    DeriveJsonCodec.gen[AuthResponse]

final case class RefreshTokenRequest(
  refreshToken: String
)

object RefreshTokenRequest:
  given JsonCodec[RefreshTokenRequest] =
    DeriveJsonCodec.gen[RefreshTokenRequest]
