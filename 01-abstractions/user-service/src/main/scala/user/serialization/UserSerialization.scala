package user.serialization

import zio.*

trait UserSerialization:
  def accessTokenCodec: Task[JsonCodec[AccessToken]]
  def refreshTokenCodec: Task[JsonCodec[RefreshToken]]
  def userCodec: Task[JsonCodec[User]]
  def userIdCodec: Task[JsonCodec[UserId]]
  def registerUserRequestCodec: Task[JsonCodec[RegisterUserRequest]]
  def loginRequestCodec: Task[JsonCodec[LoginRequest]]
  def updateUserRequestCodec: Task[JsonCodec[UpdateUserRequest]]
  def changePasswordRequestCodec: Task[JsonCodec[ChangePasswordRequest]]
  def userResponseCodec: Task[JsonCodec[UserResponse]]
  def authResponseCodec: Task[JsonCodec[AuthResponse]]
  def refreshTokenRequestCodec: Task[JsonCodec[RefreshTokenRequest]]
  def instantCodec: Task[JsonCodec[Instant]]

object UserSerialization:
  def accessTokenCodec: ZIO[UserSerialization, Nothing, JsonCodec[AccessToken]] =
    ZIO.serviceWithZIO[UserSerialization](_.accessTokenCodec)
  def refreshTokenCodec: ZIO[UserSerialization, Nothing, JsonCodec[RefreshToken]] =
    ZIO.serviceWithZIO[UserSerialization](_.refreshTokenCodec)
  def userCodec: ZIO[UserSerialization, Nothing, JsonCodec[User]] =
    ZIO.serviceWithZIO[UserSerialization](_.userCodec)
  def userIdCodec: ZIO[UserSerialization, Nothing, JsonCodec[UserId]] =
    ZIO.serviceWithZIO[UserSerialization](_.userIdCodec)
  def registerUserRequestCodec: ZIO[UserSerialization, Nothing, JsonCodec[RegisterUserRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.registerUserRequestCodec)
  def loginRequestCodec: ZIO[UserSerialization, Nothing, JsonCodec[LoginRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.loginRequestCodec)
  def updateUserRequestCodec: ZIO[UserSerialization, Nothing, JsonCodec[UpdateUserRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.updateUserRequestCodec)
  def changePasswordRequestCodec: ZIO[UserSerialization, Nothing, JsonCodec[ChangePasswordRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.changePasswordRequestCodec)
  def userResponseCodec: ZIO[UserSerialization, Nothing, JsonCodec[UserResponse]] =
    ZIO.serviceWithZIO[UserSerialization](_.userResponseCodec)
  def authResponseCodec: ZIO[UserSerialization, Nothing, JsonCodec[AuthResponse]] =
    ZIO.serviceWithZIO[UserSerialization](_.authResponseCodec)
  def refreshTokenRequestCodec: ZIO[UserSerialization, Nothing, JsonCodec[RefreshTokenRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.refreshTokenRequestCodec)
  def instantCodec: ZIO[UserSerialization, Nothing, JsonCodec[Instant]] =
    ZIO.serviceWithZIO[UserSerialization](_.instantCodec)
