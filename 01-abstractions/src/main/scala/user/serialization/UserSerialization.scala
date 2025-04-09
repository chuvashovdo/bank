package user.serialization

import zio.*
import java.time.Instant
import jwt.models.*
import zio.json.*
import user.models.*
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
  def accessTokenCodec: RIO[UserSerialization, JsonCodec[AccessToken]] =
    ZIO.serviceWithZIO[UserSerialization](_.accessTokenCodec)
  def refreshTokenCodec: RIO[UserSerialization, JsonCodec[RefreshToken]] =
    ZIO.serviceWithZIO[UserSerialization](_.refreshTokenCodec)
  def userCodec: RIO[UserSerialization, JsonCodec[User]] =
    ZIO.serviceWithZIO[UserSerialization](_.userCodec)
  def userIdCodec: RIO[UserSerialization, JsonCodec[UserId]] =
    ZIO.serviceWithZIO[UserSerialization](_.userIdCodec)
  def registerUserRequestCodec: RIO[UserSerialization, JsonCodec[RegisterUserRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.registerUserRequestCodec)
  def loginRequestCodec: RIO[UserSerialization, JsonCodec[LoginRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.loginRequestCodec)
  def updateUserRequestCodec: RIO[UserSerialization, JsonCodec[UpdateUserRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.updateUserRequestCodec)
  def changePasswordRequestCodec: RIO[UserSerialization, JsonCodec[ChangePasswordRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.changePasswordRequestCodec)
  def userResponseCodec: RIO[UserSerialization, JsonCodec[UserResponse]] =
    ZIO.serviceWithZIO[UserSerialization](_.userResponseCodec)
  def authResponseCodec: RIO[UserSerialization, JsonCodec[AuthResponse]] =
    ZIO.serviceWithZIO[UserSerialization](_.authResponseCodec)
  def refreshTokenRequestCodec: RIO[UserSerialization, JsonCodec[RefreshTokenRequest]] =
    ZIO.serviceWithZIO[UserSerialization](_.refreshTokenRequestCodec)
  def instantCodec: RIO[UserSerialization, JsonCodec[Instant]] =
    ZIO.serviceWithZIO[UserSerialization](_.instantCodec)
