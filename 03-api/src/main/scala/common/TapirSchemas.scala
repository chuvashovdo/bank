package common

import sttp.tapir.Schema
import sttp.tapir.SchemaType
import user.models.*
import jwt.models.*
import zio.json.{ JsonDecoder, JsonEncoder }

object TapirSchemas:
  implicit val schemaEmail: Schema[Email] =
    Schema(SchemaType.SString(), format = Some("email"))
  implicit val encoderEmail: JsonEncoder[Email] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderEmail: JsonDecoder[Email] =
    JsonDecoder.string.mapOrFail(str => Email(str).left.map(_.developerFriendlyMessage))

  implicit val schemaPassword: Schema[Password] =
    Schema(SchemaType.SString(), format = Some("password"))
  implicit val encoderPassword: JsonEncoder[Password] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderPassword: JsonDecoder[Password] =
    JsonDecoder.string.mapOrFail(str => Password(str).left.map(_.developerFriendlyMessage))

  implicit val schemaFirstName: Schema[FirstName] =
    Schema(SchemaType.SString(), format = Some("first_name"))
  implicit val encoderFirstName: JsonEncoder[FirstName] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderFirstName: JsonDecoder[FirstName] =
    JsonDecoder.string.mapOrFail(str => FirstName(str).left.map(_.developerFriendlyMessage))

  implicit val schemaLastName: Schema[LastName] =
    Schema(SchemaType.SString(), format = Some("last_name"))
  implicit val encoderLastName: JsonEncoder[LastName] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderLastName: JsonDecoder[LastName] =
    JsonDecoder.string.mapOrFail(str => LastName(str).left.map(_.developerFriendlyMessage))

  implicit val schemaUserId: Schema[UserId] =
    Schema(SchemaType.SString(), format = Some("user_id"))
  implicit val encoderUserId: JsonEncoder[UserId] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderUserId: JsonDecoder[UserId] =
    JsonDecoder.string.mapOrFail(str => UserId(str).left.map(_.developerFriendlyMessage))

  implicit val schemaJwtAccessToken: Schema[JwtAccessToken] =
    Schema(SchemaType.SString(), format = Some("jwt_access_token"))
  implicit val encoderJwtAccessToken: JsonEncoder[JwtAccessToken] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderJwtAccessToken: JsonDecoder[JwtAccessToken] =
    JsonDecoder.string.mapOrFail(str => JwtAccessToken(str).left.map(_.developerFriendlyMessage))

  implicit val schemaJwtRefreshToken: Schema[JwtRefreshToken] =
    Schema(SchemaType.SString(), format = Some("jwt_refresh_token"))
  implicit val encoderJwtRefreshToken: JsonEncoder[JwtRefreshToken] =
    JsonEncoder.string.contramap(_.value)
  implicit val decoderJwtRefreshToken: JsonDecoder[JwtRefreshToken] =
    JsonDecoder.string.mapOrFail(str => JwtRefreshToken(str).left.map(_.developerFriendlyMessage))

  implicit lazy val schemaRegisterUserRequest: Schema[RegisterUserRequest] =
    Schema.derived[RegisterUserRequest]
  implicit lazy val schemaLoginRequest: Schema[LoginRequest] =
    Schema.derived[LoginRequest]
  implicit lazy val schemaUpdateUserRequest: Schema[UpdateUserRequest] =
    Schema.derived[UpdateUserRequest]
  implicit lazy val schemaChangePasswordRequest: Schema[ChangePasswordRequest] =
    Schema.derived[ChangePasswordRequest]
  implicit lazy val schemaUserResponse: Schema[UserResponse] =
    Schema.derived[UserResponse]
  implicit lazy val schemaAuthResponse: Schema[AuthResponse] =
    Schema.derived[AuthResponse]
  implicit lazy val schemaRefreshTokenRequest: Schema[RefreshTokenRequest] =
    Schema.derived[RefreshTokenRequest]
