package user.api

import auth.service.AuthService
import common.models.ErrorResponse
import common.TapirSchemas.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import user.models.*
import user.models.{ UpdateUserRequest, ChangePasswordRequest }
import user.service.*
import user.mapper.UserResponseMapper
import jwt.service.JwtService
import zio.*

/** Объект-компаньон для хранения констант путей */
object UserAccountEndpoints:
  private val profilePath =
    "/api/users/me"
  private val changePasswordPath =
    s"$profilePath/password"

/** Эндпоинты для управления пользовательским аккаунтом */
class UserAccountEndpoints(
  userService: UserService,
  authService: AuthService,
  userResponseMapper: UserResponseMapper,
  jwtService: JwtService,
) extends ApiEndpoint:
  import UserAccountEndpoints.* // Импортируем пути из объекта-компаньона

  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  val getCurrentUserEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in("api" / "users" / "me")
      .summary("Получение информации о текущем аутентифицированном пользователе")
      .out(jsonBody[UserResponse])
      .serverLogic { userId => _ =>
        handleGetCurrentUser(userId).either
      }

  val updateUserEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .patch
      .in("api" / "users" / "me")
      .summary("Обновление информации о текущем пользователе (имя, фамилия)")
      .in(jsonBody[UpdateUserRequest])
      .out(jsonBody[UserResponse])
      .serverLogic { userId => request =>
        handleUpdateUser(userId, request).either
      }

  val changePasswordEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .patch
      .in("api" / "users" / "me" / "password")
      .summary("Изменение пароля текущего пользователя")
      .in(jsonBody[ChangePasswordRequest])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => request =>
        handleChangePassword(userId, request).either
      }

  val deactivateAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .delete
      .in("api" / "users" / "me")
      .summary("Деактивация учетной записи текущего пользователя")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => _ =>
        handleDeactivateAccount(userId).either
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      getCurrentUserEndpoint,
      updateUserEndpoint,
      changePasswordEndpoint,
      deactivateAccountEndpoint,
    )

  // --- Private handler methods for endpoint logic ---

  private def findAndMapUserToResponse(
    userId: UserId,
    path: String,
  ): ZIO[Any, ErrorResponse, UserResponse] =
    for
      userOpt <-
        userService
          .findUserById(userId)
          .mapError { e =>
            createErrorResponse(
              StatusCode.InternalServerError.code,
              "USER_LOOKUP_ERROR",
              s"Failed to lookup user by ID: ${e.getMessage}",
              path,
            )
          }
      user <-
        ZIO
          .fromOption(userOpt)
          .orElseFail(
            createErrorResponse(
              StatusCode.NotFound.code,
              "USER_NOT_FOUND",
              "User not found.",
              path,
            )
          )
      userResponse <-
        userResponseMapper.fromUser(user).mapError { e =>
          createErrorResponse(
            StatusCode.InternalServerError.code,
            "RESPONSE_MAPPING_ERROR",
            s"Failed to map user response: ${e.getMessage}",
            path,
          )
        }
    yield userResponse

  private def handleGetCurrentUser(userId: UserId): ZIO[Any, ErrorResponse, UserResponse] =
    findAndMapUserToResponse(userId, profilePath)

  private def handleUpdateUser(
    userId: UserId,
    request: UpdateUserRequest,
  ): ZIO[Any, ErrorResponse, UserResponse] =
    for
      // Сначала обновляем пользователя
      updatedUserOpt <-
        userService
          .updateUser(userId, request.firstName, request.lastName)
          .mapError(handleCommonErrors(profilePath))
      // Убедимся, что пользователь был обновлен и существует
      _ <-
        ZIO
          .fromOption(updatedUserOpt)
          .orElseFail:
            createErrorResponse(
              StatusCode.NotFound.code, // Или другая подходящая ошибка, если обновление не удалось по другой причине
              "USER_UPDATE_FAILED_OR_NOT_FOUND",
              "User not found or update failed.",
              profilePath,
            )
      // Затем получаем обновленного пользователя и мапим его
      userResponse <- findAndMapUserToResponse(userId, profilePath)
    yield userResponse

  private def handleChangePassword(
    userId: UserId,
    request: ChangePasswordRequest,
  ): ZIO[Any, ErrorResponse, Unit] =
    for
      success <-
        userService
          .changePassword(userId, request.oldPassword, request.newPassword)
          .mapError(handleCommonErrors(changePasswordPath))

      _ <-
        ZIO.unless(success):
          ZIO.fail:
            createErrorResponse(
              StatusCode.BadRequest.code,
              "INVALID_OLD_PASSWORD",
              "The old password provided is incorrect.",
              changePasswordPath,
            )
    yield ()

  private def handleDeactivateAccount(userId: UserId): ZIO[Any, ErrorResponse, Unit] =
    for
      success <-
        userService
          .deactivateUser(userId)
          .mapError { e =>
            createErrorResponse(
              StatusCode.InternalServerError.code,
              "DEACTIVATION_FAILED",
              s"Failed to deactivate user: ${e.getMessage}",
              profilePath,
            )
          }

      _ <-
        ZIO.unless(success):
          ZIO.fail:
            createErrorResponse(
              StatusCode.BadRequest.code,
              "DEACTIVATION_FAILED",
              "Failed to deactivate account, user might not exist or an error occurred.",
              profilePath,
            )

      _ <-
        authService
          .logout(userId)
          .mapError { e =>
            createErrorResponse(
              StatusCode.InternalServerError.code,
              "LOGOUT_POST_DEACTIVATION_FAILED",
              s"Failed to logout user after deactivation: ${e.getMessage}",
              profilePath,
            )
          }
    yield ()
