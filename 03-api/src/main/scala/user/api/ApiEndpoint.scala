package user.api

// import auth.service.AuthService
import common.errors.*
import common.models.{ ErrorDetail, ErrorResponse }
import jwt.models.JwtAccessToken
import jwt.service.JwtService
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.generic.auto.*
import user.models.UserId
import zio.*

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
      case e: UserAlreadyExistsError =>
        createErrorResponse(StatusCode.Conflict.code, e.errorCode, e.message, path)
      case e: InvalidOldPasswordError =>
        createErrorResponse(StatusCode.BadRequest.code, e.errorCode, e.message, path)
      case e: BusinessError =>
        createErrorResponse(StatusCode.BadRequest.code, e.errorCode, e.message, path)
      case NonFatal(e) =>
        createErrorResponse(
          StatusCode.InternalServerError.code,
          "INTERNAL_SERVER_ERROR",
          e.getMessage,
          path,
        )

  protected def handleValidationErrors(
    path: String,
    message: String = "Validation failed",
  )(
    errors: Seq[common.errors.ValidationError]
  ): ErrorResponse =
    ErrorResponse.fromValidationErrors(
      StatusCode.BadRequest.code,
      "VALIDATION_ERROR",
      path,
      message,
      errors.toList,
    )
