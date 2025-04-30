package user.repository

import java.time.Instant
import java.util.UUID
import user.entity.UserEntity
import user.models.User
import user.models.UserId
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import user.mapper.UserEntityMapper

class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase], userEntityMapper: UserEntityMapper)
    extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[UserEntity] =
    schemaMeta("users")

  override def findById(id: UserId): Task[Option[User]] =
    for
      userEntity <- run(quote(query[UserEntity].filter(_.id == lift(id.value))))
      oneUser <- ZIO.succeed(userEntity.headOption)
      user <-
        oneUser match
          case Some(userEntity) => userEntityMapper.toUser(userEntity).map(Some(_))
          case None => ZIO.succeed(None)
    yield user

  override def findByEmail(email: String): Task[Option[User]] =
    for
      userEntity <- run(quote(query[UserEntity].filter(_.email == lift(email))))
      oneUser <- ZIO.succeed(userEntity.headOption)
      user <-
        oneUser match
          case Some(userEntity) => userEntityMapper.toUser(userEntity).map(Some(_))
          case None => ZIO.succeed(None)
    yield user

  override def create(
    email: String,
    passwordHash: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User] =
    for
      user <-
        ZIO.succeed(
          User(
            id = UserId(UUID.randomUUID().toString),
            email,
            passwordHash,
            firstName,
            lastName,
            isActive = true,
          )
        )
      userEntity <- userEntityMapper.fromUser(user, Instant.now(), Instant.now())
      _ <-
        run:
          quote:
            query[UserEntity].insertValue:
              lift(userEntity)
    yield user

  override def update(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[Option[User]] =
    for
      existingUserOpt <- findById(id)
      result <-
        existingUserOpt match
          case Some(existingUser) =>
            val now = Instant.now()
            val updatedUser =
              existingUser.copy(
                firstName = firstName,
                lastName = lastName,
              )
            val createdAt =
              run(quote {
                query[UserEntity].filter(_.id == lift(id.value)).map(_.createdAt)
              }).map(_.headOption.getOrElse(now)).orDie

            for
              created <- createdAt
              entity <- userEntityMapper.fromUser(updatedUser, created, now)
              _ <-
                run:
                  quote:
                    query[UserEntity]
                      .filter(_.id == lift(id.value))
                      .update(
                        _.firstName -> lift(entity.firstName),
                        _.lastName -> lift(entity.lastName),
                        _.updatedAt -> lift(entity.updatedAt),
                      )
            yield Some(updatedUser)
          case None =>
            ZIO.none
    yield result

  override def updatePassword(id: UserId, passwordHash: String): Task[Boolean] =
    for
      existingUserOpt <- findById(id)
      result <-
        existingUserOpt match
          case Some(user) =>
            run {
              quote:
                query[UserEntity]
                  .filter(_.id == lift(id.value))
                  .update(_.passwordHash -> lift(passwordHash), _.updatedAt -> lift(Instant.now()))
            }.map(_ => true)
          case None =>
            ZIO.succeed(false)
    yield result

  override def deactivate(id: UserId): Task[Boolean] =
    for
      existingUserOpt <- findById(id)
      result <-
        existingUserOpt match
          case Some(_) =>
            val now = Instant.now()
            run {
              quote:
                query[UserEntity]
                  .filter(_.id == lift(id.value))
                  .update(
                    _.isActive -> lift(false),
                    _.updatedAt -> lift(now),
                  )
            }.map(_ > 0)
          case None =>
            ZIO.succeed(false)
    yield result

object UserRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase] & UserEntityMapper, UserRepository] =
    ZLayer:
      for
        postgres <- ZIO.service[Quill.Postgres[SnakeCase]]
        userEntityMapper <- ZIO.service[UserEntityMapper]
      yield new UserRepositoryImpl(postgres, userEntityMapper)
