package common.api

import common.errors.*
import auth.errors.*
import common.models.{ ErrorDetail, ErrorResponse }
import jwt.models.{ AuthContext, JwtAccessToken }
import jwt.service.JwtService
import sttp.model.StatusCode
import sttp.tapir.{ Endpoint, auth, endpoint, * }
import sttp.tapir.json.zio.*
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.generic.auto.*
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
  ): PartialServerEndpoint[String, AuthContext, Unit, ErrorResponse, Unit, Any, Task] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic[AuthContext, Task] { tokenString =>
        (for
          token <-
            ZIO.fromEither(JwtAccessToken(tokenString)).mapError { _ =>
              createInvalidTokenFormatError("/authentication")
            }
          authContext <- jwtService.validateToken(token)
        yield authContext)
          .map(Right(_))
          .catchAll:
            case er: ErrorResponse => ZIO.succeed(Left(er))
            case _ =>
              ZIO.succeed(Left(createUnauthorizedError("/authentication")))
          .catchAllDefect { _ =>
            ZIO.succeed(Left(createTokenValidationError("/authentication")))
          }
      }

  protected def createSecuredEndpointWithRoles(
    jwtService: JwtService,
    roles: String*
  ): PartialServerEndpoint[String, AuthContext, Unit, ErrorResponse, Unit, Any, Task] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic[AuthContext, Task] { tokenString =>
        (for
          token <-
            ZIO.fromEither(JwtAccessToken(tokenString)).mapError { _ =>
              createInvalidTokenFormatError("/authentication")
            }
          authContext <- jwtService.validateToken(token)

          _ <- ZIO.fromEither(checkRolesInternal(authContext, roles))
        yield authContext)
          .map(Right(_))
          .catchAll:
            case er: ErrorResponse => ZIO.succeed(Left(er))
            case _ =>
              ZIO.succeed(Left(createUnauthorizedError("/authentication")))
          .catchAllDefect { _ =>
            ZIO.succeed(Left(createTokenValidationError("/authentication")))
          }
      }

  protected def createSecuredEndpointWithPermissions(
    jwtService: JwtService,
    permissions: String*
  ): PartialServerEndpoint[String, AuthContext, Unit, ErrorResponse, Unit, Any, Task] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic[AuthContext, Task] { tokenString =>
        (for
          token <-
            ZIO.fromEither(JwtAccessToken(tokenString)).mapError { _ =>
              createInvalidTokenFormatError("/authentication")
            }
          authContext <- jwtService.validateToken(token)

          _ <- ZIO.fromEither(checkPermissionsInternal(authContext, permissions))
        yield authContext)
          .map(Right(_))
          .catchAll:
            case er: ErrorResponse => ZIO.succeed(Left(er))
            case _ =>
              ZIO.succeed(Left(createUnauthorizedError("/authentication")))
          .catchAllDefect { _ =>
            ZIO.succeed(Left(createTokenValidationError("/authentication")))
          }
      }

  protected def createSecuredEndpointWithRoleOrPermission(
    jwtService: JwtService,
    roles: Seq[String],
    permissions: Seq[String],
  ): PartialServerEndpoint[String, AuthContext, Unit, ErrorResponse, Unit, Any, Task] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic[AuthContext, Task] { tokenString =>
        (for
          token <-
            ZIO.fromEither(JwtAccessToken(tokenString)).mapError { _ =>
              createInvalidTokenFormatError("/authentication")
            }
          authContext <- jwtService.validateToken(token)

          _ <-
            ZIO.fromEither(
              checkRolesAnyInternal(authContext, roles)
                .orElse(checkPermissionsInternal(authContext, permissions))
            )
        yield authContext)
          .map(Right(_))
          .catchAll:
            case er: ErrorResponse => ZIO.succeed(Left(er))
            case _ =>
              ZIO.succeed(Left(createUnauthorizedError("/authentication")))
          .catchAllDefect { _ =>
            ZIO.succeed(Left(createTokenValidationError("/authentication")))
          }
      }

  private def checkRolesInternal(
    authContext: AuthContext,
    requiredRoles: Seq[String],
  ): Either[ErrorResponse, Unit] =
    val userRoles = authContext.roles.map(_.name)
    val hasAllRoles = requiredRoles.toSet.subsetOf(userRoles)
    if hasAllRoles then Right(())
    else Left(createForbiddenError("/authorization"))

  // метод для проверки наличия ЛЮБОЙ из указанных ролей
  private def checkRolesAnyInternal(
    authContext: AuthContext,
    requiredRoles: Seq[String],
  ): Either[ErrorResponse, Unit] =
    if requiredRoles.isEmpty then Right(())
    else
      val userRoles = authContext.roles.map(_.name)
      val hasAnyRole = requiredRoles.exists(role => userRoles.contains(role))
      if hasAnyRole then Right(())
      else Left(createForbiddenError("/authorization"))

  private def checkPermissionsInternal(
    authContext: AuthContext,
    requiredPermissions: Seq[String],
  ): Either[ErrorResponse, Unit] =
    if requiredPermissions.isEmpty then Right(())
    else
      val userPermissions = authContext.permissions.map(_.name)
      val hasAllPermissions = requiredPermissions.toSet.subsetOf(userPermissions)
      if hasAllPermissions then Right(())
      else
        Left:
          createForbiddenError(
            "/authorization",
            "You don't have the required permissions to perform this action.",
          )

  // метод для проверки наличия ЛЮБОГО из указанных разрешений
  // private def checkPermissionsAnyInternal(
  //   authContext: AuthContext,
  //   requiredPermissions: Seq[String],
  // ): Either[ErrorResponse, Unit] =
  //   if requiredPermissions.isEmpty then Right(())
  //   else
  //     val userPermissions = authContext.permissions.map(_.name)
  //     val hasAnyPermission =
  //       requiredPermissions.exists(permission => userPermissions.contains(permission))
  //     if hasAnyPermission then Right(())
  //     else
  //       Left:
  //         createErrorResponse(
  //           StatusCode.Forbidden.code,
  //           "FORBIDDEN",
  //           "You don't have any of the required permissions to perform this action.",
  //           "/authorization",
  //         )

  protected def checkRoles(
    authContext: AuthContext,
    path: String,
    requiredRoles: String*
  ): Either[ErrorResponse, AuthContext] =
    val userRoles = authContext.roles.map(_.name)
    val hasAllRoles = requiredRoles.toSet.subsetOf(userRoles)
    if hasAllRoles then Right(authContext)
    else Left(createForbiddenError(path))

  protected def checkAnyRoles(
    authContext: AuthContext,
    path: String,
    requiredRoles: String*
  ): Either[ErrorResponse, AuthContext] =
    if requiredRoles.isEmpty then Right(authContext)
    else
      val userRoles = authContext.roles.map(_.name)
      val hasAnyRole = requiredRoles.exists(userRoles.contains)
      if hasAnyRole then Right(authContext)
      else Left(createForbiddenError(path))

  protected def checkPermissions(
    authContext: AuthContext,
    path: String,
    requiredPermissions: String*
  ): Either[ErrorResponse, AuthContext] =
    val userPermissions = authContext.permissions.map(_.name)
    val hasAllPermissions = requiredPermissions.toSet.subsetOf(userPermissions)
    if hasAllPermissions then Right(authContext)
    else
      Left(
        createForbiddenError(
          path,
          "You don't have the required permissions to perform this action.",
        )
      )

  protected def checkAnyPermissions(
    authContext: AuthContext,
    path: String,
    requiredPermissions: String*
  ): Either[ErrorResponse, AuthContext] =
    if requiredPermissions.isEmpty then Right(authContext)
    else
      val userPermissions = authContext.permissions.map(_.name)
      val hasAnyPermission = requiredPermissions.exists(userPermissions.contains)
      if hasAnyPermission then Right(authContext)
      else
        Left(
          createForbiddenError(
            path,
            "You don't have any of the required permissions to perform this action.",
          )
        )

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

  private def createForbiddenError(
    path: String,
    message: String = "You are not authorized to perform this action.",
  ): ErrorResponse =
    createErrorResponse(
      StatusCode.Forbidden.code,
      "FORBIDDEN",
      message,
      path,
    )

  private def createUnauthorizedError(
    path: String,
    message: String = "Invalid or expired token.",
  ): ErrorResponse =
    createErrorResponse(
      StatusCode.Unauthorized.code,
      "AUTHENTICATION_FAILED",
      message,
      path,
    )

  private def createInvalidTokenFormatError(path: String): ErrorResponse =
    createErrorResponse(
      StatusCode.Unauthorized.code,
      "INVALID_TOKEN_FORMAT",
      "Invalid token format.",
      path,
    )

  private def createTokenValidationError(path: String): ErrorResponse =
    createErrorResponse(
      StatusCode.InternalServerError.code,
      "TOKEN_VALIDATION_ERROR",
      "An unexpected error occurred during token validation.",
      path,
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
