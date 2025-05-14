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
import common.errors.UserNotFoundError
class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase], userEntityMapper: UserEntityMapper)
    extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[UserEntity] =
    schemaMeta("users")

  override def findById(id: String): Task[User] =
    for
      userEntityOpt <-
        run(quote(query[UserEntity].filter(_.id == lift(id)).take(1)))
          .map(_.headOption)
      user <-
        ZIO
          .fromOption(userEntityOpt)
          .mapError { _ => UserNotFoundError(id) }
          .flatMap(userEntityMapper.toUser)
    yield user

  override def findByEmail(emailRaw: String): Task[User] =
    for
      userEntityOpt <-
        run(quote(query[UserEntity].filter(_.email == lift(emailRaw)).take(1)))
          .map(_.headOption)
      user <-
        ZIO
          .fromOption(userEntityOpt)
          .mapError { _ => UserNotFoundError(emailRaw) }
          .flatMap(userEntityMapper.toUser)
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

      validFirstNameOpt <-
        ZIO.foreach(firstNameRaw) { name =>
          ZIO
            .fromEither(FirstName(name))
            .mapError(err =>
              new IllegalArgumentException(
                s"Invalid first name for new user: ${err.developerFriendlyMessage}"
              )
            )
        }

      validLastNameOpt <-
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
          firstName = validFirstNameOpt,
          lastName = validLastNameOpt,
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
    id: String,
    firstNameRaw: Option[String],
    lastNameRaw: Option[String],
  ): Task[User] =
    for
      existingUser: User <- findById(id)

      validFirstNameOpt: Option[FirstName] <-
        ZIO.foreach(firstNameRaw) { name =>
          ZIO
            .fromEither(FirstName(name))
            .mapError(e =>
              new IllegalArgumentException(
                s"Invalid first name for update: ${e.developerFriendlyMessage}"
              )
            )
        }
      validLastNameOpt: Option[LastName] <-
        ZIO.foreach(lastNameRaw) { name =>
          ZIO
            .fromEither(LastName(name))
            .mapError(e =>
              new IllegalArgumentException(
                s"Invalid last name for update: ${e.developerFriendlyMessage}"
              )
            )
        }

      now = Instant.now()

      updatedUserDomain: User =
        existingUser.copy(
          firstName = validFirstNameOpt.orElse(existingUser.firstName),
          lastName = validLastNameOpt.orElse(existingUser.lastName),
        )

      _ <-
        run:
          quote:
            query[UserEntity]
              .filter(_.id == lift(id))
              .update(
                setValue => setValue.firstName -> lift(validFirstNameOpt.map(_.value)),
                setValue => setValue.lastName -> lift(validLastNameOpt.map(_.value)),
                _.updatedAt -> lift(now),
              )
    yield updatedUserDomain

  override def updatePassword(id: String, passwordHash: String): Task[Unit] =
    for
      _ <- findById(id)
      _ <-
        run:
          quote:
            query[UserEntity]
              .filter(_.id == lift(id))
              .update(_.passwordHash -> lift(passwordHash), _.updatedAt -> lift(Instant.now()))
    yield ()

  override def deactivate(id: String): Task[Unit] =
    for
      _ <- findById(id)
      now = Instant.now()
      _ <-
        run(
          quote(
            query[UserEntity]
              .filter(_.id == lift(id))
              .update(
                _.isActive -> lift(false),
                _.updatedAt -> lift(now),
              )
          )
        )
    yield ()

object UserRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase] & UserEntityMapper, UserRepository] =
    ZLayer:
      for
        postgres <- ZIO.service[Quill.Postgres[SnakeCase]]
        userEntityMapper <- ZIO.service[UserEntityMapper]
      yield new UserRepositoryImpl(postgres, userEntityMapper)
