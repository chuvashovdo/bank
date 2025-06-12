package user.api

import auth.service.AuthService
import common.models.ErrorResponse
import common.TapirSchemas.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import user.models.*
import user.models.dto.{ UpdateUserRequest, ChangePasswordRequest, UserResponse }
import user.service.*
import jwt.service.JwtService
import zio.*
import common.api.ApiEndpoint

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
  jwtService: JwtService,
) extends ApiEndpoint:
  import UserAccountEndpoints.* // Импортируем пути из объекта-компаньона

  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  val getCurrentUserEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in("api" / "users" / "me")
      .tag("User Account")
      .summary("Получение информации о текущем аутентифицированном пользователе")
      .out(jsonBody[UserResponse])
      .serverLogic { userId => _ =>
        handleGetCurrentUser(userId).either
      }

  val updateUserEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .patch
      .in("api" / "users" / "me")
      .tag("User Account")
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
      .tag("User Account")
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
      .tag("User Account")
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
    userService
      .findUserById(userId)
      .mapError(handleCommonErrors(path))
      .map(user => UserResponse(user.id, user.email, user.firstName, user.lastName))

  private def handleGetCurrentUser(userId: UserId): ZIO[Any, ErrorResponse, UserResponse] =
    findAndMapUserToResponse(userId, profilePath)

  private def handleUpdateUser(
    userId: UserId,
    request: UpdateUserRequest,
  ): ZIO[Any, ErrorResponse, UserResponse] =
    for
      updatedUser <-
        userService
          .updateUser(userId, request.firstName, request.lastName)
          .mapError(handleCommonErrors(profilePath))
      userResponse <- findAndMapUserToResponse(userId, profilePath)
    yield userResponse

  private def handleChangePassword(
    userId: UserId,
    request: ChangePasswordRequest,
  ): ZIO[Any, ErrorResponse, Unit] =
    userService
      .changePassword(userId, request.oldPassword, request.newPassword)
      .mapError(handleCommonErrors(changePasswordPath))

  private def handleDeactivateAccount(userId: UserId): ZIO[Any, ErrorResponse, Unit] =
    for
      _ <-
        userService
          .deactivateUser(userId)
          .mapError(handleCommonErrors(profilePath))

      _ <-
        authService
          .logout(userId)
          .mapError(handleCommonErrors(profilePath))
    yield ()
