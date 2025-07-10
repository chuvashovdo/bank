package user.api

import zio.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint

import common.api.ApiEndpoint
import common.models.ErrorResponse
import common.TapirSchemas.given
import jwt.service.JwtService
import user.service.UserService
import user.models.{ UserId, RoleId }
import user.models.dto.{ UserResponse, UpdateUserAdminRequest }
import user.models.{ FirstName, LastName }
import user.mapper.UserMapper

class UserAdminEndpoints(
  userService: UserService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val adminUsersBasePath =
    "api" / "admin" / "users"

  private val adminSecuredEndpoint =
    createSecuredEndpointWithRoles(jwtService, "ADMIN")

  val listUsersEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminUsersBasePath)
      .tag("Admin - User Management")
      .summary("Получить список всех пользователей (только для администратора)")
      .out(jsonBody[List[UserResponse]])
      .serverLogic { authContext => _ =>
        handleListUsers().either
      }

  val getUserByIdEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminUsersBasePath / path[UserId]("userId"))
      .tag("Admin - User Management")
      .summary("Получить информацию о пользователе по ID (только для администратора)")
      .out(jsonBody[UserResponse])
      .serverLogic { authContext => userId =>
        handleGetUserById(userId).either
      }

  val updateUserEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .put
      .in(adminUsersBasePath / path[UserId]("userId"))
      .tag("Admin - User Management")
      .summary("Обновить информацию о пользователе (только для администратора)")
      .in(jsonBody[UpdateUserAdminRequest])
      .out(jsonBody[UserResponse])
      .serverLogic { authContext =>
        {
          case (userId, updateRequest) =>
            handleUpdateUser(
              userId,
              updateRequest.firstName,
              updateRequest.lastName,
              updateRequest.roles,
            ).either
        }
      }

  val deleteUserEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(adminUsersBasePath / path[UserId]("userId"))
      .tag("Admin - User Management")
      .summary("Удалить пользователя (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => userId =>
        handleDeleteUser(userId)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      listUsersEndpoint,
      getUserByIdEndpoint,
      updateUserEndpoint,
      deleteUserEndpoint,
    )

  private def handleListUsers(): ZIO[Any, ErrorResponse, List[UserResponse]] =
    userService
      .findAllUsers()
      .map(_.map(UserMapper.toResponseFromModel(_)))
      .mapError(handleCommonErrors(adminUsersBasePath.toString()))

  private def handleGetUserById(userId: UserId): ZIO[Any, ErrorResponse, UserResponse] =
    userService
      .findUserById(userId)
      .map(UserMapper.toResponseFromModel(_))
      .mapError(handleCommonErrors(s"${adminUsersBasePath.toString()}/$userId"))

  private def handleUpdateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
    roles: Option[Set[RoleId]],
  ): ZIO[Any, ErrorResponse, UserResponse] =
    for
      updatedUser <-
        userService
          .updateUser(id, firstName, lastName)
          .mapError(handleCommonErrors(s"${adminUsersBasePath.toString()}/$id"))

      _ <-
        roles match
          case Some(newRoleIds) =>
            userService
              .updateUserRoles(id, newRoleIds)
              .mapError(handleCommonErrors(s"${adminUsersBasePath.toString()}/$id"))
          case None => ZIO.unit

      userWithUpdatedRoles <-
        userService
          .findUserById(id)
          .mapError(handleCommonErrors(s"${adminUsersBasePath.toString()}/$id"))
    yield UserMapper.toResponseFromModel(userWithUpdatedRoles)

  private def handleDeleteUser(userId: UserId): ZIO[Any, ErrorResponse, Unit] =
    userService
      .deactivateUser(userId)
      .mapError(handleCommonErrors(s"${adminUsersBasePath.toString()}/$userId"))
