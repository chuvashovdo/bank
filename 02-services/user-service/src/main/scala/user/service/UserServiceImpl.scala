package user.service

import user.entity.UserEntity
import user.mapper.UserMapper
import user.models.*
import user.repository.UserRepository
import zio.*
import org.mindrot.jbcrypt.BCrypt
import user.errors.*
import java.util.UUID
import java.time.Instant

class UserServiceImpl(userRepository: UserRepository) extends UserService:
  override def findUserById(id: UserId): Task[User] =
    userRepository.findById(id.value).flatMap(UserMapper.toModel)

  override def findUserByEmail(email: Email): Task[User] =
    userRepository.findByEmail(email.value).flatMap(UserMapper.toModel)

  override def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    userRepository
      .findByEmail(email.value)
      .foldZIO(
        failure = {
          case _: UserNotFoundError =>
            for
              passwordHash <- hashPassword(password.value)
              now = Instant.now()
              userEntity =
                UserEntity(
                  id = UUID.randomUUID(),
                  email = email.value,
                  passwordHash = passwordHash,
                  firstName = firstName.map(_.value),
                  lastName = lastName.map(_.value),
                  isActive = true,
                  createdAt = now,
                  updatedAt = now,
                )
              createdEntity <- userRepository.create(userEntity)
              user <- UserMapper.toModel(createdEntity)
            yield user
          case otherError => ZIO.fail(otherError)
        },
        success = _ => ZIO.fail(UserAlreadyExistsError(email.value)),
      )

  override def validateCredentials(email: Email, password: Password): Task[User] =
    for
      userEntity <- userRepository.findByEmail(email.value)
      user <- UserMapper.toModel(userEntity)
      _ <- ZIO.fail(UserNotActiveError(user.id.value)).when(!user.isActive)
      isValid <- checkPassword(password.value, user.passwordHash)
      _ <- ZIO.fail(InvalidCredentialsError()).when(!isValid)
    yield user

  override def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    for
      existingEntity <- userRepository.findById(id.value)
      now = Instant.now()
      updatedEntity =
        existingEntity.copy(
          firstName = firstName.map(_.value).orElse(existingEntity.firstName),
          lastName = lastName.map(_.value).orElse(existingEntity.lastName),
          updatedAt = now,
        )
      resultEntity <- userRepository.update(updatedEntity)
      user <- UserMapper.toModel(resultEntity)
    yield user

  override def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Unit] =
    for
      userEntity <- userRepository.findById(id.value)
      user <- UserMapper.toModel(userEntity)
      _ <- ZIO.fail(UserNotActiveError(user.id.value)).when(!user.isActive)
      isValid <- checkPassword(oldPassword.value, user.passwordHash)
      _ <- ZIO.fail(InvalidOldPasswordError(id.value)).when(!isValid)
      newHash <- hashPassword(newPassword.value)
      _ <- userRepository.updatePassword(id.value, newHash)
    yield ()

  override def deactivateUser(id: UserId): Task[Unit] =
    for
      _ <- userRepository.findById(id.value)
      _ <- userRepository.deactivate(id.value)
    yield ()

  private def hashPassword(passwordRaw: String): Task[String] =
    ZIO.attempt(BCrypt.hashpw(passwordRaw, BCrypt.gensalt(12)))

  private def checkPassword(passwordRaw: String, hashedPassword: String): Task[Boolean] =
    ZIO.attempt(BCrypt.checkpw(passwordRaw, hashedPassword))

object UserServiceImpl:
  val layer: URLayer[UserRepository, UserService] =
    ZLayer:
      for userRepository <- ZIO.service[UserRepository]
      yield new UserServiceImpl(userRepository)
