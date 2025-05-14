package common

import sttp.tapir.Schema
import sttp.tapir.SchemaType
import user.models.*
import jwt.models.*
import zio.json.{ JsonDecoder, JsonEncoder }

object TapirSchemas:
  given schemaEmail: Schema[Email] =
    Schema(SchemaType.SString(), format = Some("email"))
  given encoderEmail: JsonEncoder[Email] =
    JsonEncoder.string.contramap(_.value)
  given decoderEmail: JsonDecoder[Email] =
    JsonDecoder.string.mapOrFail(str => Email(str).left.map(_.developerFriendlyMessage))

  given schemaPassword: Schema[Password] =
    Schema(SchemaType.SString(), format = Some("password"))
  given encoderPassword: JsonEncoder[Password] =
    JsonEncoder.string.contramap(_.value)
  given decoderPassword: JsonDecoder[Password] =
    JsonDecoder.string.mapOrFail(str => Password(str).left.map(_.developerFriendlyMessage))

  given schemaFirstName: Schema[FirstName] =
    Schema(SchemaType.SString(), format = Some("first_name"))
  given encoderFirstName: JsonEncoder[FirstName] =
    JsonEncoder.string.contramap(_.value)
  given decoderFirstName: JsonDecoder[FirstName] =
    JsonDecoder.string.mapOrFail(str => FirstName(str).left.map(_.developerFriendlyMessage))

  given schemaLastName: Schema[LastName] =
    Schema(SchemaType.SString(), format = Some("last_name"))
  given encoderLastName: JsonEncoder[LastName] =
    JsonEncoder.string.contramap(_.value)
  given decoderLastName: JsonDecoder[LastName] =
    JsonDecoder.string.mapOrFail(str => LastName(str).left.map(_.developerFriendlyMessage))

  given schemaUserId: Schema[UserId] =
    Schema(SchemaType.SString(), format = Some("user_id"))
  given encoderUserId: JsonEncoder[UserId] =
    JsonEncoder.string.contramap(_.value)
  given decoderUserId: JsonDecoder[UserId] =
    JsonDecoder.string.mapOrFail(str => UserId(str).left.map(_.developerFriendlyMessage))

  given schemaJwtAccessToken: Schema[JwtAccessToken] =
    Schema(SchemaType.SString(), format = Some("jwt_access_token"))
  given encoderJwtAccessToken: JsonEncoder[JwtAccessToken] =
    JsonEncoder.string.contramap(_.value)
  given decoderJwtAccessToken: JsonDecoder[JwtAccessToken] =
    JsonDecoder.string.mapOrFail(str => JwtAccessToken(str).left.map(_.developerFriendlyMessage))

  given schemaJwtRefreshToken: Schema[JwtRefreshToken] =
    Schema(SchemaType.SString(), format = Some("jwt_refresh_token"))
  given encoderJwtRefreshToken: JsonEncoder[JwtRefreshToken] =
    JsonEncoder.string.contramap(_.value)
  given decoderJwtRefreshToken: JsonDecoder[JwtRefreshToken] =
    JsonDecoder.string.mapOrFail(str => JwtRefreshToken(str).left.map(_.developerFriendlyMessage))

  given schemaRegisterUserRequest: Schema[RegisterUserRequest] =
    Schema.derived[RegisterUserRequest]
  given schemaLoginRequest: Schema[LoginRequest] =
    Schema.derived[LoginRequest]
  given schemaUpdateUserRequest: Schema[UpdateUserRequest] =
    Schema.derived[UpdateUserRequest]
  given schemaChangePasswordRequest: Schema[ChangePasswordRequest] =
    Schema.derived[ChangePasswordRequest]
  given schemaUserResponse: Schema[UserResponse] =
    Schema.derived[UserResponse]
  given schemaAuthResponse: Schema[AuthResponse] =
    Schema.derived[AuthResponse]
  given schemaRefreshTokenRequest: Schema[RefreshTokenRequest] =
    Schema.derived[RefreshTokenRequest]
