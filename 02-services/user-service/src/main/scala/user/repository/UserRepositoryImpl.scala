package user.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import user.entity.UserEntity
import user.errors.*
import user.mapper.UserMapper
import user.models.{ Email, FirstName, LastName, User, UserId }
import zio.*

import java.time.Instant
import java.util.UUID

class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[UserEntity] =
    schemaMeta("users")

  override def findById(id: UUID): Task[User] =
    for
      userEntityOpt <-
        run(quote(query[UserEntity].filter(_.id.equals(lift(id))).take(1))).map(_.headOption)
      userEntity <- ZIO.fromOption(userEntityOpt).mapError(_ => UserNotFoundError(id))
      user <- UserMapper.toModel(userEntity)
    yield user

  override def findByEmail(emailRaw: String): Task[User] =
    for
      userEntityOpt <-
        run(quote(query[UserEntity].filter(_.email == lift(emailRaw)).take(1))).map(_.headOption)
      userEntity <- ZIO.fromOption(userEntityOpt).mapError(_ => UserNotFoundError(emailRaw))
      user <- UserMapper.toModel(userEntity)
    yield user

  override def create(
    id: UUID,
    emailRaw: String,
    passwordHash: String,
    firstNameRaw: Option[String],
    lastNameRaw: Option[String],
  ): Task[User] =
    for
      validEmail <-
        ZIO.fromEither(Email(emailRaw)).mapError(e => InvalidEmailError(e.developerFriendlyMessage))
      validFirstNameOpt <-
        ZIO.foreach(firstNameRaw)(name =>
          ZIO
            .fromEither(FirstName(name))
            .mapError(e => InvalidFirstNameError(e.developerFriendlyMessage))
        )
      validLastNameOpt <-
        ZIO.foreach(lastNameRaw)(name =>
          ZIO
            .fromEither(LastName(name))
            .mapError(e => InvalidLastNameError(e.developerFriendlyMessage))
        )
      user =
        User(
          id = UserId(id),
          email = validEmail,
          passwordHash = passwordHash,
          firstName = validFirstNameOpt,
          lastName = validLastNameOpt,
          isActive = true,
        )
      now = Instant.now()
      userEntity = UserMapper.toEntity(user).copy(createdAt = now, updatedAt = now)
      _ <- run(quote(query[UserEntity].insertValue(lift(userEntity))))
    yield user

  override def update(
    id: UUID,
    firstNameRaw: Option[String],
    lastNameRaw: Option[String],
  ): Task[User] =
    for
      existingUser <- findById(id)
      validFirstNameOpt <-
        ZIO.foreach(firstNameRaw)(name =>
          ZIO
            .fromEither(FirstName(name))
            .mapError(e => InvalidFirstNameError(e.developerFriendlyMessage))
        )
      validLastNameOpt <-
        ZIO.foreach(lastNameRaw)(name =>
          ZIO
            .fromEither(LastName(name))
            .mapError(e => InvalidLastNameError(e.developerFriendlyMessage))
        )
      now = Instant.now()
      updatedUserDomain =
        existingUser.copy(
          firstName = validFirstNameOpt.orElse(existingUser.firstName),
          lastName = validLastNameOpt.orElse(existingUser.lastName),
        )
      _ <-
        run:
          quote:
            query[UserEntity]
              .filter(_.id.equals(lift(id)))
              .update(
                _.firstName -> lift(updatedUserDomain.firstName.map(_.value)),
                _.lastName -> lift(updatedUserDomain.lastName.map(_.value)),
                _.updatedAt -> lift(now),
              )
    yield updatedUserDomain

  override def updatePassword(id: UUID, passwordHash: String): Task[Unit] =
    for
      _ <- findById(id)
      _ <-
        run:
          quote:
            query[UserEntity]
              .filter(_.id.equals(lift(id)))
              .update(_.passwordHash -> lift(passwordHash), _.updatedAt -> lift(Instant.now()))
    yield ()

  override def deactivate(id: UUID): Task[Unit] =
    for
      _ <- findById(id)
      now = Instant.now()
      _ <-
        run(
          quote(
            query[UserEntity]
              .filter(_.id.equals(lift(id)))
              .update(_.isActive -> lift(false), _.updatedAt -> lift(now))
          )
        )
    yield ()

object UserRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], UserRepository] =
    ZLayer.fromFunction(new UserRepositoryImpl(_))
