package user.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import user.entity.UserEntity
import user.errors.*
import zio.*

import java.time.Instant
import java.util.UUID

class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[UserEntity] =
    schemaMeta("app_users")

  case class UserRoleEntity(userId: UUID, roleId: UUID)
  inline given userRoleSchemaMeta: SchemaMeta[UserRoleEntity] =
    schemaMeta("app_user_roles", _.userId -> "user_id", _.roleId -> "role_id")

  override def findById(id: UUID): Task[UserEntity] =
    run(quote(query[UserEntity].filter(_.id.equals(lift(id))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => UserNotFoundError(id)))

  override def findByEmail(email: String): Task[UserEntity] =
    run(quote(query[UserEntity].filter(_.email.equals(lift(email))).take(1)))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => UserNotFoundError(email)))

  override def create(user: UserEntity): Task[UserEntity] =
    run(quote(query[UserEntity].insertValue(lift(user)))).as(user)

  override def update(user: UserEntity): Task[UserEntity] =
    run(
      quote(
        query[UserEntity]
          .filter(_.id.equals(lift(user.id)))
          .updateValue(lift(user))
      )
    ).as(user)

  override def updatePassword(id: UUID, passwordHash: String): Task[Unit] =
    run {
      quote:
        query[UserEntity]
          .filter(_.id.equals(lift(id)))
          .update(_.passwordHash -> lift(passwordHash), _.updatedAt -> lift(Instant.now()))
    }.unit

  override def deactivate(id: UUID): Task[Unit] =
    run(
      quote(
        query[UserEntity]
          .filter(_.id.equals(lift(id)))
          .update(_.isActive -> lift(false), _.updatedAt -> lift(Instant.now()))
      )
    ).unit

  override def findAll(): Task[List[UserEntity]] =
    run(quote(query[UserEntity]))

  override def addRoleToUser(userId: UUID, roleId: UUID): Task[Unit] =
    run(quote(query[UserRoleEntity].insertValue(lift(UserRoleEntity(userId, roleId))))).unit

  override def removeRoleFromUser(userId: UUID, roleId: UUID): Task[Unit] =
    run(
      quote(
        query[UserRoleEntity]
          .filter(ur => ur.userId.equals(lift(userId)) && ur.roleId.equals(lift(roleId)))
          .delete
      )
    ).unit

  override def findUserRoleIds(userId: UUID): Task[List[UUID]] =
    run(quote(query[UserRoleEntity].filter(_.userId.equals(lift(userId))).map(_.roleId)))

object UserRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], UserRepository] =
    ZLayer.fromFunction(UserRepositoryImpl(_))
