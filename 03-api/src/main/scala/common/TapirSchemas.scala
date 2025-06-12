package common

import sttp.tapir.Schema
import sttp.tapir.SchemaType
import user.models.*
import jwt.models.*
import zio.json.{ JsonDecoder, JsonEncoder }
import user.models.dto.{
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RefreshTokenRequest,
  RegisterUserRequest,
  UpdateUserRequest,
  UserResponse,
}
import bank.models.Balance
import bank.models.AccountStatus
import bank.models.dto.*
import scala.math.BigDecimal
import scala.util.Try
import sttp.tapir.{ Codec, CodecFormat }
import bank.models.{ AccountId, TransactionId }

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
    JsonEncoder.string.contramap(_.value.toString)
  given decoderUserId: JsonDecoder[UserId] =
    JsonDecoder.string.mapOrFail { str =>
      try Right(UserId(java.util.UUID.fromString(str)))
      catch case _: IllegalArgumentException => Left("Invalid UUID format for UserId")
    }

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

  // --- Bank Schemas ---

  given schemaBalance: Schema[Balance] =
    Schema.schemaForBigDecimal.map(Balance.apply(_).toOption)(_.value)

  given schemaAccountStatus: Schema[AccountStatus] =
    Schema.derivedEnumeration[AccountStatus].defaultStringBased
  given encoderAccountStatus: JsonEncoder[AccountStatus] =
    JsonEncoder.string.contramap(_.toString)
  given decoderAccountStatus: JsonDecoder[AccountStatus] =
    JsonDecoder
      .string
      .mapOrFail(s =>
        Try(AccountStatus.valueOf(s)).toEither.left.map(_ => "Invalid account status")
      )

  // --- User DTOs ---

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

  // --- Bank DTOs ---

  given schemaCreateAccountRequest: Schema[CreateAccountRequest] =
    Schema.derived[CreateAccountRequest]
  given schemaAccountResponse: Schema[AccountResponse] =
    Schema.derived[AccountResponse]
  given schemaTransactionRequest: Schema[TransactionRequest] =
    Schema.derived[TransactionRequest]
  given schemaTransferRequest: Schema[TransferRequest] =
    Schema.derived[TransferRequest]
  given schemaTransferByAccountRequest: Schema[TransferByAccountRequest] =
    Schema.derived[TransferByAccountRequest]
  given schemaTransactionResponse: Schema[TransactionResponse] =
    Schema.derived[TransactionResponse]

  given schemaAccountId: Schema[AccountId] =
    Schema(SchemaType.SString(), format = Some("account_id"))
  given encoderAccountId: JsonEncoder[AccountId] =
    JsonEncoder.string.contramap(_.value.toString)
  given decoderAccountId: JsonDecoder[AccountId] =
    JsonDecoder.string.mapOrFail { str =>
      try Right(AccountId(java.util.UUID.fromString(str)))
      catch case _: IllegalArgumentException => Left("Invalid UUID format for AccountId")
    }

  given schemaTransactionId: Schema[TransactionId] =
    Schema(SchemaType.SString(), format = Some("transaction_id"))
  given encoderTransactionId: JsonEncoder[TransactionId] =
    JsonEncoder.string.contramap(_.value.toString)
  given decoderTransactionId: JsonDecoder[TransactionId] =
    JsonDecoder.string.mapOrFail { str =>
      try Right(TransactionId(java.util.UUID.fromString(str)))
      catch case _: IllegalArgumentException => Left("Invalid UUID format for TransactionId")
    }

  // --- Path Codecs ---
  given Codec[String, UserId, CodecFormat.TextPlain] =
    Codec.uuid.map(UserId.apply)(_.value)
  given Codec[String, AccountId, CodecFormat.TextPlain] =
    Codec.uuid.map(AccountId.apply)(_.value)
  given Codec[String, TransactionId, CodecFormat.TextPlain] =
    Codec.uuid.map(TransactionId.apply)(_.value)
