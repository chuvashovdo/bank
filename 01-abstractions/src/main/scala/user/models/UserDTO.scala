package user.model

final case class RegisterUserRequest(
  email: String,
  password: String,
  firstName: String,
  lastName: String,
)

final case class LoginRequest(
  email: String,
  password: String,
)

final case class UpdateUserRequest(
  firstName: Option[String],
  lastName: Option[String],
)

final case class ChangePasswordRequest(
  oldPassword: String,
  newPassword: String,
)

final case class UserResponse(
  id: String,
  email: String,
  firstName: String,
  lastName: String,
)

final case class AuthResponse(
  accessToken: String,
  refreshToken: String,
  expiresAt: Long,
  user: UserResponse,
)

final case class RefreshTokenRequest(
  refreshToken: String
)
