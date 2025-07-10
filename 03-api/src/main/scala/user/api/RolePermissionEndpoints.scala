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
import user.service.{ PermissionService, RoleService }
import user.models.{ Permission, PermissionId, Role, RoleId }
import user.models.dto.{
  CreateRoleRequest,
  UpdateRoleRequest,
  RoleResponse,
  PermissionResponse,
  CreatePermissionRequest,
  UpdatePermissionRequest,
}

class RolePermissionEndpoints(
  roleService: RoleService,
  permissionService: PermissionService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val rolesBasePath =
    "api" / "admin" / "roles"
  private val permissionsBasePath =
    "api" / "admin" / "permissions"

  private val adminSecuredEndpoint =
    createSecuredEndpointWithRoles(jwtService, "ADMIN")

  val createRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .post
      .in(rolesBasePath)
      .tag("Admin - Roles")
      .summary("Создать новую роль (только для администратора)")
      .in(jsonBody[CreateRoleRequest])
      .out(jsonBody[RoleResponse])
      .serverLogic { authContext => request =>
        handleCreateRole(request).either
      }

  val listRolesEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(rolesBasePath)
      .tag("Admin - Roles")
      .summary("Получить список всех ролей (только для администратора)")
      .out(jsonBody[List[RoleResponse]])
      .serverLogic { authContext => _ =>
        handleListRoles().either
      }

  val getRoleByIdEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(rolesBasePath / path[RoleId]("roleId"))
      .tag("Admin - Roles")
      .summary("Получить информацию о роли по ID (только для администратора)")
      .out(jsonBody[RoleResponse])
      .serverLogic { authContext => roleId =>
        handleGetRoleById(roleId).either
      }

  val getRoleByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(rolesBasePath / "name" / path[String]("roleName"))
      .tag("Admin - Roles")
      .summary("Получить информацию о роли по имени (только для администратора)")
      .out(jsonBody[RoleResponse])
      .serverLogic { authContext => roleName =>
        handleGetRoleByName(roleName).either
      }

  val updateRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .put
      .in(rolesBasePath / path[RoleId]("roleId"))
      .tag("Admin - Roles")
      .summary("Обновить информацию о роли (только для администратора)")
      .in(jsonBody[UpdateRoleRequest])
      .out(jsonBody[RoleResponse])
      .serverLogic { authContext =>
        {
          case (roleId, request) =>
            handleUpdateRole(roleId, request.name, request.description).either
        }
      }

  val deleteRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(rolesBasePath / path[RoleId]("roleId"))
      .tag("Admin - Roles")
      .summary("Удалить роль (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => roleId =>
        handleDeleteRole(roleId)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val addPermissionToRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .post
      .in(
        rolesBasePath / path[RoleId]("roleId") / "permissions" / path[PermissionId]("permissionId")
      )
      .tag("Admin - Role Permissions")
      .summary("Добавить разрешение к роли (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext =>
        {
          case (roleId: RoleId, permissionId: PermissionId) =>
            handleAddPermissionToRole(roleId, permissionId)
              .map(_ => Right(()))
              .catchAll(err => ZIO.succeed(Left(err)))
        }
      }

  val removePermissionFromRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(
        rolesBasePath / path[RoleId]("roleId") / "permissions" / path[PermissionId]("permissionId")
      )
      .tag("Admin - Role Permissions")
      .summary("Удалить разрешение из роли (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext =>
        {
          case (roleId: RoleId, permissionId: PermissionId) =>
            handleRemovePermissionFromRole(roleId, permissionId)
              .map(_ => Right(()))
              .catchAll(err => ZIO.succeed(Left(err)))
        }
      }

  val getPermissionsForRoleEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(rolesBasePath / path[RoleId]("roleId") / "permissions")
      .tag("Admin - Role Permissions")
      .summary("Получить список разрешений для роли (только для администратора)")
      .out(jsonBody[List[PermissionResponse]])
      .serverLogic { authContext => roleId =>
        handleGetPermissionsForRole(roleId).either
      }

  val createPermissionEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .post
      .in(permissionsBasePath)
      .tag("Admin - Permissions")
      .summary("Создать новое разрешение (только для администратора)")
      .in(jsonBody[CreatePermissionRequest])
      .out(jsonBody[PermissionResponse])
      .serverLogic { authContext => request =>
        handleCreatePermission(request).either
      }

  val listPermissionsEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(permissionsBasePath)
      .tag("Admin - Permissions")
      .summary("Получить список всех разрешений (только для администратора)")
      .out(jsonBody[List[PermissionResponse]])
      .serverLogic { authContext => _ =>
        handleListPermissions().either
      }

  val getPermissionByIdEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(permissionsBasePath / path[PermissionId]("permissionId"))
      .tag("Admin - Permissions")
      .summary("Получить информацию о разрешении по ID (только для администратора)")
      .out(jsonBody[PermissionResponse])
      .serverLogic { authContext => permissionId =>
        handleGetPermissionById(permissionId).either
      }

  val getPermissionByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(permissionsBasePath / "name" / path[String]("permissionName"))
      .tag("Admin - Permissions")
      .summary("Получить информацию о разрешении по имени (только для администратора)")
      .out(jsonBody[PermissionResponse])
      .serverLogic { authContext => permissionName =>
        handleGetPermissionByName(permissionName).either
      }

  val updatePermissionEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .put
      .in(permissionsBasePath / path[PermissionId]("permissionId"))
      .tag("Admin - Permissions")
      .summary("Обновить информацию о разрешении (только для администратора)")
      .in(jsonBody[UpdatePermissionRequest])
      .out(jsonBody[PermissionResponse])
      .serverLogic { authContext =>
        {
          case (permissionId, request) =>
            handleUpdatePermission(permissionId, request.name, request.description).either
        }
      }

  val deletePermissionEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(permissionsBasePath / path[PermissionId]("permissionId"))
      .tag("Admin - Permissions")
      .summary("Удалить разрешение (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => permissionId =>
        handleDeletePermission(permissionId)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val addPermissionToRoleByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .post
      .in(
        rolesBasePath / "name" / path[String]("roleName") / "permissions" / "name" / path[String](
          "permissionName"
        )
      )
      .tag("Admin - Role Permissions")
      .summary("Добавить разрешение к роли по именам (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext =>
        {
          case (roleName: String, permissionName: String) =>
            handleAddPermissionToRoleByName(roleName, permissionName)
              .map(_ => Right(()))
              .catchAll(err => ZIO.succeed(Left(err)))
        }
      }

  val removePermissionFromRoleByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(
        rolesBasePath / "name" / path[String]("roleName") / "permissions" / "name" / path[String](
          "permissionName"
        )
      )
      .tag("Admin - Role Permissions")
      .summary("Удалить разрешение из роли по именам (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext =>
        {
          case (roleName: String, permissionName: String) =>
            handleRemovePermissionFromRoleByName(roleName, permissionName)
              .map(_ => Right(()))
              .catchAll(err => ZIO.succeed(Left(err)))
        }
      }

  val updatePermissionByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .put
      .in(permissionsBasePath / "name" / path[String]("permissionName"))
      .tag("Admin - Permissions")
      .summary("Обновить информацию о разрешении по имени (только для администратора)")
      .in(jsonBody[UpdatePermissionRequest])
      .out(jsonBody[PermissionResponse])
      .serverLogic { authContext =>
        {
          case (permissionName: String, request: UpdatePermissionRequest) =>
            handleUpdatePermissionByName(permissionName, request.name, request.description).either
        }
      }

  val deletePermissionByNameEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(permissionsBasePath / "name" / path[String]("permissionName"))
      .tag("Admin - Permissions")
      .summary("Удалить разрешение по имени (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => permissionName =>
        handleDeletePermissionByName(permissionName)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      createRoleEndpoint,
      listRolesEndpoint,
      getRoleByIdEndpoint,
      updateRoleEndpoint,
      deleteRoleEndpoint,
      addPermissionToRoleEndpoint,
      removePermissionFromRoleEndpoint,
      getPermissionsForRoleEndpoint,
      getRoleByNameEndpoint,
      addPermissionToRoleByNameEndpoint,
      removePermissionFromRoleByNameEndpoint,
      createPermissionEndpoint,
      listPermissionsEndpoint,
      getPermissionByIdEndpoint,
      updatePermissionEndpoint,
      deletePermissionEndpoint,
      getPermissionByNameEndpoint,
      updatePermissionByNameEndpoint,
      deletePermissionByNameEndpoint,
    )

  private def handleCreateRole(
    request: CreateRoleRequest
  ): ZIO[Any, ErrorResponse, RoleResponse] =
    roleService
      .createRole(request.name, request.description)
      .map(roleToResponse)
      .mapError(handleCommonErrors(rolesBasePath.toString()))

  private def handleListRoles(): ZIO[Any, ErrorResponse, List[RoleResponse]] =
    roleService
      .findAllRoles()
      .map(_.map(roleToResponse))
      .mapError(handleCommonErrors(rolesBasePath.toString()))

  private def handleGetRoleById(roleId: RoleId): ZIO[Any, ErrorResponse, RoleResponse] =
    roleService
      .findRoleById(roleId)
      .map(roleToResponse)
      .mapError(handleCommonErrors(s"${rolesBasePath.toString()}/$roleId"))

  private def handleGetRoleByName(name: String): ZIO[Any, ErrorResponse, RoleResponse] =
    roleService
      .findRoleByName(name)
      .map(roleToResponse)
      .mapError(handleCommonErrors(s"${rolesBasePath.toString()}/name/$name"))

  private def handleUpdateRole(
    id: RoleId,
    name: String,
    description: Option[String],
  ): ZIO[Any, ErrorResponse, RoleResponse] =
    roleService
      .updateRole(id, name, description)
      .map(roleToResponse)
      .mapError(handleCommonErrors(s"${rolesBasePath.toString()}/$id"))

  private def handleDeleteRole(roleId: RoleId): ZIO[Any, ErrorResponse, Unit] =
    roleService
      .deleteRole(roleId)
      .mapError(handleCommonErrors(s"${rolesBasePath.toString()}/$roleId"))

  private def handleAddPermissionToRole(
    roleId: RoleId,
    permissionId: PermissionId,
  ): ZIO[Any, ErrorResponse, Unit] =
    roleService
      .addPermissionToRole(roleId, permissionId)
      .mapError(
        handleCommonErrors(s"${rolesBasePath.toString()}/$roleId/permissions/$permissionId")
      )

  private def handleRemovePermissionFromRole(
    roleId: RoleId,
    permissionId: PermissionId,
  ): ZIO[Any, ErrorResponse, Unit] =
    roleService
      .removePermissionFromRole(roleId, permissionId)
      .mapError(
        handleCommonErrors(s"${rolesBasePath.toString()}/$roleId/permissions/$permissionId")
      )

  private def handleGetPermissionsForRole(
    roleId: RoleId
  ): ZIO[Any, ErrorResponse, List[PermissionResponse]] =
    roleService
      .findPermissionsForRole(roleId)
      .map(_.map(permissionToResponse).toList)
      .mapError(handleCommonErrors(s"${rolesBasePath.toString()}/$roleId/permissions"))

  private def handleCreatePermission(
    request: CreatePermissionRequest
  ): ZIO[Any, ErrorResponse, PermissionResponse] =
    permissionService
      .createPermission(request.name, request.description)
      .map(permissionToResponse)
      .mapError(handleCommonErrors(permissionsBasePath.toString()))

  private def handleListPermissions(): ZIO[Any, ErrorResponse, List[PermissionResponse]] =
    permissionService
      .findAllPermissions()
      .map(_.map(permissionToResponse))
      .mapError(handleCommonErrors(permissionsBasePath.toString()))

  private def handleGetPermissionById(
    permissionId: PermissionId
  ): ZIO[Any, ErrorResponse, PermissionResponse] =
    permissionService
      .findPermissionById(permissionId)
      .map(permissionToResponse)
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/$permissionId"))

  private def handleGetPermissionByName(
    name: String
  ): ZIO[Any, ErrorResponse, PermissionResponse] =
    permissionService
      .findPermissionByName(name)
      .map(permissionToResponse)
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/name/$name"))

  private def handleUpdatePermission(
    id: PermissionId,
    name: String,
    description: Option[String],
  ): ZIO[Any, ErrorResponse, PermissionResponse] =
    permissionService
      .updatePermission(id, name, description)
      .map(permissionToResponse)
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/$id"))

  private def handleDeletePermission(
    permissionId: PermissionId
  ): ZIO[Any, ErrorResponse, Unit] =
    permissionService
      .deletePermission(permissionId)
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/$permissionId"))

  private def handleAddPermissionToRoleByName(
    roleName: String,
    permissionName: String,
  ): ZIO[Any, ErrorResponse, Unit] =
    (for
      role <- roleService.findRoleByName(roleName)
      permission <- permissionService.findPermissionByName(permissionName)
      _ <- roleService.addPermissionToRole(role.id, permission.id)
    yield ())
      .mapError(
        handleCommonErrors(
          s"${rolesBasePath.toString()}/name/$roleName/permissions/name/$permissionName"
        )
      )

  private def handleRemovePermissionFromRoleByName(
    roleName: String,
    permissionName: String,
  ): ZIO[Any, ErrorResponse, Unit] =
    (for
      role <- roleService.findRoleByName(roleName)
      permission <- permissionService.findPermissionByName(permissionName)
      _ <- roleService.removePermissionFromRole(role.id, permission.id)
    yield ())
      .mapError(
        handleCommonErrors(
          s"${rolesBasePath.toString()}/name/$roleName/permissions/name/$permissionName"
        )
      )

  private def handleUpdatePermissionByName(
    permissionName: String,
    newName: String,
    description: Option[String],
  ): ZIO[Any, ErrorResponse, PermissionResponse] =
    (for
      permission <- permissionService.findPermissionByName(permissionName)
      updatedPermission <- permissionService.updatePermission(permission.id, newName, description)
    yield permissionToResponse(updatedPermission))
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/name/$permissionName"))

  private def handleDeletePermissionByName(
    permissionName: String
  ): ZIO[Any, ErrorResponse, Unit] =
    (for
      permission <- permissionService.findPermissionByName(permissionName)
      _ <- permissionService.deletePermission(permission.id)
    yield ())
      .mapError(handleCommonErrors(s"${permissionsBasePath.toString()}/name/$permissionName"))

  private def roleToResponse(role: Role): RoleResponse =
    RoleResponse(
      id = role.id,
      name = role.name,
      description = role.description,
      permissions = role.permissions.map(permissionToResponse).toList,
    )

  private def permissionToResponse(permission: Permission): PermissionResponse =
    PermissionResponse(
      id = permission.id,
      name = permission.name,
      description = permission.description,
    )
