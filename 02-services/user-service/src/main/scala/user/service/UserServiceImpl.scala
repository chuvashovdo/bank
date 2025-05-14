package user.service

import user.models.{ User, UserId, Email, Password, FirstName, LastName }
import user.repository.UserRepository
import zio.*
import org.mindrot.jbcrypt.BCrypt
import common.errors.{
  UserAlreadyExistsError,
  InvalidCredentialsError,
  UserNotActiveError,
  InvalidOldPasswordError,
  UserNotFoundError,
}

class UserServiceImpl(userRepository: UserRepository) extends UserService:
  override def findUserById(id: UserId): Task[User] =
    userRepository.findById(id.value)

  override def findUserByEmail(email: Email): Task[User] =
    userRepository.findByEmail(email.value)

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
              newUser <-
                userRepository.create(
                  email.value,
                  passwordHash,
                  firstName.map(_.value),
                  lastName.map(_.value),
                )
            yield newUser
          case otherError => ZIO.fail(otherError)
        },
        success = _ => ZIO.fail(UserAlreadyExistsError(email)),
      )

  override def validateCredentials(email: Email, password: Password): Task[User] =
    for
      user <- userRepository.findByEmail(email.value)
      _ <- ZIO.fail(UserNotActiveError(email.value)).when(!user.isActive)
      isValid <- checkPassword(password.value, user.passwordHash)
      _ <- ZIO.fail(InvalidCredentialsError()).when(!isValid)
    yield user

  override def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    userRepository.update(id.value, firstName.map(_.value), lastName.map(_.value))

  override def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Unit] =
    for
      user <- userRepository.findById(id.value)
      _ <- ZIO.fail(UserNotActiveError(id.value)).when(!user.isActive)
      isValid <- checkPassword(oldPassword.value, user.passwordHash)
      _ <- ZIO.fail(InvalidOldPasswordError(id)).when(!isValid)
      newHash <- hashPassword(newPassword.value)
      _ <- userRepository.updatePassword(id.value, newHash)
    yield ()

  override def deactivateUser(id: UserId): Task[Unit] =
    for
      user <- userRepository.findById(id.value)
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
