package user.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import user.entity.RoleEntity
import user.errors.RoleNotFoundError
import zio.*

import java.time.Instant
import java.util.UUID

class RoleRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends RoleRepository:
  import quill.*

  inline given roleSchemaMeta: SchemaMeta[RoleEntity] =
    schemaMeta("app_roles")

  case class RolePermissionEntity(roleId: UUID, permissionId: UUID)
  inline given rolePermissionSchemaMeta: SchemaMeta[RolePermissionEntity] =
    schemaMeta("app_role_permissions", _.roleId -> "role_id", _.permissionId -> "permission_id")

  override def findById(id: UUID): Task[RoleEntity] =
    run(quote(query[RoleEntity].filter(_.id.equals(lift(id))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => RoleNotFoundError(id)))

  override def findByName(name: String): Task[RoleEntity] =
    run(quote(query[RoleEntity].filter(_.name.equals(lift(name))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => RoleNotFoundError(name)))

  override def create(role: RoleEntity): Task[RoleEntity] =
    run(quote(query[RoleEntity].insertValue(lift(role)))).as(role)

  override def update(role: RoleEntity): Task[RoleEntity] =
    run(
      quote(
        query[RoleEntity]
          .filter(_.id.equals(lift(role.id)))
          .updateValue(lift(role))
      )
    ).as(role)

  override def delete(id: UUID): Task[Unit] =
    run(quote(query[RoleEntity].filter(_.id.equals(lift(id))).delete)).unit

  override def findAll(): Task[List[RoleEntity]] =
    run(quote(query[RoleEntity]))

  override def addPermissionToRole(roleId: UUID, permissionId: UUID): Task[Unit] =
    run(
      quote(
        query[RolePermissionEntity].insertValue(lift(RolePermissionEntity(roleId, permissionId)))
      )
    ).unit

  override def removePermissionFromRole(roleId: UUID, permissionId: UUID): Task[Unit] =
    run {
      quote:
        query[RolePermissionEntity]
          .filter(rp =>
            rp.roleId.equals(lift(roleId)) && rp.permissionId.equals(lift(permissionId))
          )
          .delete
    }.unit

  override def findPermissionsByRoleId(roleId: UUID): Task[List[UUID]] =
    run(
      quote(query[RolePermissionEntity].filter(_.roleId.equals(lift(roleId))).map(_.permissionId))
    )

object RoleRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], RoleRepository] =
    ZLayer.fromFunction(RoleRepositoryImpl(_))
