package user.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import user.entity.PermissionEntity
import user.errors.PermissionNotFoundError
import zio.*

import java.time.Instant
import java.util.UUID

class PermissionRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends PermissionRepository:
  import quill.*

  inline given permissionSchemaMeta: SchemaMeta[PermissionEntity] =
    schemaMeta("app_permissions")

  override def findById(id: UUID): Task[PermissionEntity] =
    run(quote(query[PermissionEntity].filter(_.id.equals(lift(id))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => PermissionNotFoundError(id)))

  override def findByName(name: String): Task[PermissionEntity] =
    run(quote(query[PermissionEntity].filter(_.name.equals(lift(name))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => PermissionNotFoundError(name)))

  override def create(permission: PermissionEntity): Task[PermissionEntity] =
    run(quote(query[PermissionEntity].insertValue(lift(permission)))).as(permission)

  override def update(permission: PermissionEntity): Task[PermissionEntity] =
    run(
      quote(
        query[PermissionEntity]
          .filter(_.id.equals(lift(permission.id)))
          .updateValue(lift(permission))
      )
    ).as(permission)

  override def delete(id: UUID): Task[Unit] =
    run(quote(query[PermissionEntity].filter(_.id.equals(lift(id))).delete)).unit

  override def findAll(): Task[List[PermissionEntity]] =
    run(quote(query[PermissionEntity]))

object PermissionRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], PermissionRepository] =
    ZLayer.fromFunction(PermissionRepositoryImpl(_))
