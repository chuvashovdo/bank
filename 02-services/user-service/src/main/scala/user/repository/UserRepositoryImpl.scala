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
    schemaMeta("users")

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

object UserRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], UserRepository] =
    ZLayer.fromFunction(new UserRepositoryImpl(_))
