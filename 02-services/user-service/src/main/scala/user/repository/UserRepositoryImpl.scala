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
import user.models.{ Email, FirstName, LastName }

class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase], userEntityMapper: UserEntityMapper)
    extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[UserEntity] =
    schemaMeta("users")

  override def findById(id: UserId): Task[Option[User]] =
    for
      userEntity <- run(quote(query[UserEntity].filter(_.id == lift(id.value))))
      oneUser <- ZIO.succeed(userEntity.headOption)
      user <- ZIO.foreach(oneUser)(userEntityMapper.toUser(_))
    yield user

  override def findByEmail(emailRaw: String): Task[Option[User]] =
    for
      userEntity <- run(quote(query[UserEntity].filter(_.email == lift(emailRaw))))
      oneUser <- ZIO.succeed(userEntity.headOption)
      user <- ZIO.foreach(oneUser)(userEntityMapper.toUser(_))
    yield user

  override def create(
    emailRaw: String,
    passwordHash: String,
    firstNameRaw: Option[String],
    lastNameRaw: Option[String],
  ): Task[User] =
    for
      validatedUserId <-
        ZIO
          .fromEither(UserId(UUID.randomUUID().toString))
          .mapError(err =>
            new IllegalArgumentException(
              s"Invalid UserId for new user: ${err.developerFriendlyMessage}"
            )
          )
      validEmail <-
        ZIO
          .fromEither(Email(emailRaw))
          .mapError(err =>
            new IllegalArgumentException(
              s"Invalid email for new user: ${err.developerFriendlyMessage}"
            )
          )

      validFirstName <-
        ZIO.foreach(firstNameRaw) { name =>
          ZIO
            .fromEither(FirstName(name))
            .mapError(err =>
              new IllegalArgumentException(
                s"Invalid first name for new user: ${err.developerFriendlyMessage}"
              )
            )
        }

      validLastName <-
        ZIO.foreach(lastNameRaw) { name =>
          ZIO
            .fromEither(LastName(name))
            .mapError(err =>
              new IllegalArgumentException(
                s"Invalid last name for new user: ${err.developerFriendlyMessage}"
              )
            )
        }
      user =
        User(
          id = validatedUserId,
          email = validEmail,
          passwordHash = passwordHash,
          firstName = validFirstName,
          lastName = validLastName,
          isActive = true,
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
    firstNameRaw: Option[String],
    lastNameRaw: Option[String],
  ): Task[Option[User]] =
    for
      existingUserOpt <- findById(id)
      result <-
        existingUserOpt match
          case Some(existingUser) =>
            for
              validFirstName <-
                ZIO.foreach(firstNameRaw) { name =>
                  ZIO
                    .fromEither(FirstName(name))
                    .mapError(err =>
                      new IllegalArgumentException(
                        s"Invalid first name for update: ${err.developerFriendlyMessage}"
                      )
                    )
                }
              validLastName <-
                ZIO.foreach(lastNameRaw) { name =>
                  ZIO
                    .fromEither(LastName(name))
                    .mapError(err =>
                      new IllegalArgumentException(
                        s"Invalid last name for update: ${err.developerFriendlyMessage}"
                      )
                    )
                }

              now = Instant.now()
              updatedUser =
                existingUser.copy(
                  firstName = validFirstName,
                  lastName = validLastName,
                )

              existingEntityCreatedAt <-
                run(quote(query[UserEntity].filter(_.id == lift(id.value)).map(_.createdAt)))
                  .map(_.headOption.getOrElse(now))

              entityToUpdate <- userEntityMapper.fromUser(updatedUser, existingEntityCreatedAt, now)

              _ <-
                run:
                  quote:
                    query[UserEntity]
                      .filter(_.id == lift(id.value))
                      .update(
                        _.firstName -> lift(entityToUpdate.firstName),
                        _.lastName -> lift(entityToUpdate.lastName),
                        _.updatedAt -> lift(entityToUpdate.updatedAt),
                      )
            yield Some(updatedUser)
          case None => ZIO.succeed(None)
    yield result

  override def updatePassword(id: UserId, passwordHash: String): Task[Boolean] =
    for
      existingUserOpt <- findById(id)
      result <-
        existingUserOpt match
          case Some(_) =>
            run {
              quote:
                query[UserEntity]
                  .filter(_.id == lift(id.value))
                  .update(_.passwordHash -> lift(passwordHash), _.updatedAt -> lift(Instant.now()))
            }.map(_ > 0)
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
            run(
              quote(
                query[UserEntity]
                  .filter(_.id == lift(id.value))
                  .update(
                    _.isActive -> lift(false),
                    _.updatedAt -> lift(now),
                  )
              )
            ).map(_ > 0)
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
