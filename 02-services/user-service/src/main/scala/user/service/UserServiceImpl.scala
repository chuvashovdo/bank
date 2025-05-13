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
  override def findUserById(id: UserId): Task[Option[User]] =
    userRepository.findById(id)

  override def findUserByEmail(email: Email): Task[Option[User]] =
    userRepository.findByEmail(email.value)

  override def registerUser(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[User] =
    for
      existingUser <- userRepository.findByEmail(email.value)
      _ <- ZIO.fail(UserAlreadyExistsError(email)).when(existingUser.isDefined)
      passwordHash <- hashPassword(password.value)
      user <-
        userRepository.create(
          email = email.value,
          passwordHash = passwordHash,
          firstName = firstName.map(_.value),
          lastName = lastName.map(_.value),
        )
    yield user

  override def validateCredentials(email: Email, password: Password): Task[Option[User]] =
    for
      userOpt <- userRepository.findByEmail(email.value)
      user <- ZIO.fromOption(userOpt).orElseFail(InvalidCredentialsError())
      _ <- ZIO.fail(UserNotActiveError(email.value)).when(!user.isActive)
      isValid <- checkPassword(password.value, user.passwordHash)
      _ <- ZIO.fail(InvalidCredentialsError()).when(!isValid)
    yield Some(user)

  override def updateUser(
    id: UserId,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[Option[User]] =
    userRepository.update(id, firstName.map(_.value), lastName.map(_.value))

  override def changePassword(
    id: UserId,
    oldPassword: Password,
    newPassword: Password,
  ): Task[Boolean] =
    for
      userOpt <- userRepository.findById(id)
      user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(id.value))
      _ <- ZIO.fail(UserNotActiveError(id.value)).when(!user.isActive)
      isValid <- checkPassword(oldPassword.value, user.passwordHash)
      _ <- ZIO.fail(InvalidOldPasswordError(id)).when(!isValid)
      newHash <- hashPassword(newPassword.value)
      updated <- userRepository.updatePassword(id, newHash)
    yield updated

  override def deactivateUser(id: UserId): Task[Boolean] =
    for
      userOpt <- userRepository.findById(id)
      _ <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(id.value))
      deactivated <- userRepository.deactivate(id)
    yield deactivated

  private def hashPassword(passwordRaw: String): Task[String] =
    ZIO.attempt(BCrypt.hashpw(passwordRaw, BCrypt.gensalt(12)))

  private def checkPassword(passwordRaw: String, hashedPassword: String): Task[Boolean] =
    ZIO.attempt(BCrypt.checkpw(passwordRaw, hashedPassword))

object UserServiceImpl:
  val layer: URLayer[UserRepository, UserService] =
    ZLayer:
      for userRepository <- ZIO.service[UserRepository]
      yield new UserServiceImpl(userRepository)
