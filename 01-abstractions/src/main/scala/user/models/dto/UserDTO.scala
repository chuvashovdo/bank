package user.models.dto

import user.models.*
import jwt.models.{ JwtAccessToken, JwtRefreshToken }
import zio.json.*

final case class RegisterUserRequest(
  email: Email,
  password: Password,
  firstName: Option[FirstName],
  lastName: Option[LastName],
)

object RegisterUserRequest:
  given JsonCodec[RegisterUserRequest] =
    DeriveJsonCodec.gen[RegisterUserRequest]

final case class LoginRequest(
  email: Email,
  password: Password,
)

object LoginRequest:
  given JsonCodec[LoginRequest] =
    DeriveJsonCodec.gen[LoginRequest]

final case class UpdateUserRequest(
  firstName: Option[FirstName],
  lastName: Option[LastName],
)

object UpdateUserRequest:
  given JsonCodec[UpdateUserRequest] =
    DeriveJsonCodec.gen[UpdateUserRequest]

final case class ChangePasswordRequest(
  oldPassword: Password,
  newPassword: Password,
)

object ChangePasswordRequest:
  given JsonCodec[ChangePasswordRequest] =
    DeriveJsonCodec.gen[ChangePasswordRequest]

final case class UserResponse(
  id: UserId,
  email: Email,
  firstName: Option[FirstName],
  lastName: Option[LastName],
)

object UserResponse:
  given JsonCodec[UserResponse] =
    DeriveJsonCodec.gen[UserResponse]

final case class AuthResponse(
  accessToken: JwtAccessToken,
  refreshToken: JwtRefreshToken,
  expiresAt: Long,
  user: UserResponse,
)

object AuthResponse:
  given JsonCodec[AuthResponse] =
    DeriveJsonCodec.gen[AuthResponse]

final case class RefreshTokenRequest(
  refreshToken: JwtRefreshToken
)

object RefreshTokenRequest:
  given JsonCodec[RefreshTokenRequest] =
    DeriveJsonCodec.gen[RefreshTokenRequest]
