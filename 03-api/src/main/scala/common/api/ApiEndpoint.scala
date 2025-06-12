package common.api

import common.errors.*
import auth.errors.*
import common.models.{ ErrorDetail, ErrorResponse }
import jwt.models.JwtAccessToken
import jwt.service.JwtService
import sttp.model.StatusCode
import sttp.tapir.{ Endpoint, auth, endpoint, * }
import sttp.tapir.json.zio.*
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.generic.auto.*
import user.models.UserId
import zio.*
import common.errors.ValidationError
import user.errors.*
import jwt.errors.*
import bank.errors.*

import java.time.Instant
import scala.util.control.NonFatal

trait ApiEndpoint:
  protected val baseEndpoint: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint.errorOut(jsonBody[ErrorResponse])

  protected def createSecuredEndpoint(
    jwtService: JwtService
  ): PartialServerEndpoint[String, UserId, Unit, ErrorResponse, Unit, Any, Task] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic { tokenString =>
        (for
          token <-
            ZIO.fromEither(JwtAccessToken(tokenString)).mapError { _ =>
              createErrorResponse(
                StatusCode.Unauthorized.code,
                "INVALID_TOKEN_FORMAT",
                "Invalid token format.",
                "/authentication",
              )
            }
          userId <- jwtService.validateToken(token)
        yield userId)
          .map(Right(_))
          .catchAll:
            case er: ErrorResponse => ZIO.succeed(Left(er))
            case _ =>
              ZIO.succeed:
                Left:
                  createErrorResponse(
                    StatusCode.Unauthorized.code,
                    "AUTHENTICATION_FAILED",
                    "Invalid or expired token.",
                    "/authentication",
                  )
          .catchAllDefect { _ =>
            ZIO.succeed:
              Left:
                createErrorResponse(
                  StatusCode.InternalServerError.code,
                  "TOKEN_VALIDATION_ERROR",
                  "An unexpected error occurred during token validation.",
                  "/authentication",
                )
          }
      }

  protected def createErrorResponse(
    status: Int,
    errorType: String,
    message: String,
    path: String,
    details: Option[Seq[ErrorDetail]] = None,
  ): ErrorResponse =
    ErrorResponse(
      timestamp = Instant.now(),
      status = status,
      errorType = errorType,
      message = message,
      path = path,
      details = details.map(_.toList),
    )

  protected def handleCommonErrors(path: String)(error: Throwable): ErrorResponse =
    error match
      // User and Auth Errors
      case e: UserAlreadyExistsError =>
        ErrorResponse.fromBusinessError(StatusCode.Conflict.code, path, e)
      case e: InvalidOldPasswordError =>
        ErrorResponse.fromBusinessError(StatusCode.BadRequest.code, path, e)
      case e: InvalidCredentialsError =>
        ErrorResponse.fromBusinessError(StatusCode.Unauthorized.code, path, e)
      case e: UserNotFoundError =>
        ErrorResponse.fromBusinessError(StatusCode.NotFound.code, path, e)
      case e: RefreshTokenNotFoundError =>
        ErrorResponse.fromBusinessError(StatusCode.NotFound.code, path, e)
      case e: UserNotActiveError =>
        ErrorResponse.fromBusinessError(StatusCode.Forbidden.code, path, e)

      // JWT Errors
      case e: TokenMissingClaimError =>
        ErrorResponse.fromBusinessError(StatusCode.Unauthorized.code, path, e)
      case e: TokenExpiredError =>
        ErrorResponse.fromBusinessError(StatusCode.Unauthorized.code, path, e)
      case e: InvalidTokenSubjectFormatError =>
        ErrorResponse.fromBusinessError(StatusCode.Unauthorized.code, path, e)
      case e: TokenDecodingError =>
        ErrorResponse.fromBusinessError(StatusCode.Unauthorized.code, path, e)

      // Bank Errors
      case e: AccountNotFoundError =>
        ErrorResponse.fromBusinessError(StatusCode.NotFound.code, path, e)
      case e: AccountClosedError =>
        ErrorResponse.fromBusinessError(StatusCode.BadRequest.code, path, e)
      case e: UnauthorizedAccountAccessError =>
        ErrorResponse.fromBusinessError(StatusCode.Forbidden.code, path, e)
      case e: InsufficientFundsError =>
        ErrorResponse.fromBusinessError(StatusCode.BadRequest.code, path, e)
      case e: CurrencyMismatchError =>
        ErrorResponse.fromBusinessError(StatusCode.BadRequest.code, path, e)

      // Generic Errors
      case e: CorruptedDataInDBError =>
        ErrorResponse.fromBusinessError(StatusCode.InternalServerError.code, path, e)
      case e: BusinessError =>
        ErrorResponse.fromBusinessError(StatusCode.BadRequest.code, path, e)
      case e: ValidationError =>
        ErrorResponse.fromValidationErrors(
          StatusCode.BadRequest.code,
          "VALIDATION_ERROR",
          path,
          "Validation error occurred",
          List(e),
        )
      case NonFatal(e) =>
        createErrorResponse(
          StatusCode.InternalServerError.code,
          "INTERNAL_SERVER_ERROR",
          e.getMessage,
          path,
        )
