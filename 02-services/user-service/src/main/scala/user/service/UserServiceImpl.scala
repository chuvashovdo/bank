package user.service

import user.models.User
import user.models.UserId
import user.repository.UserRepository
import zio.*
import org.mindrot.jbcrypt.BCrypt

class UserServiceImpl(userRepository: UserRepository) extends UserService:
  override def findUserById(id: UserId): Task[Option[User]] =
    userRepository.findById(id)

  override def findUserByEmail(email: String): Task[Option[User]] =
    userRepository.findByEmail(email)

  override def registerUser(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[User] =
    for
      existingUser <- userRepository.findByEmail(email)
      _ <- ZIO.fail(new Exception("User already exists")).when(existingUser.isDefined)
      passwordHash <- hashPassword(password)
      user <- userRepository.create(email, passwordHash, firstName, lastName)
    yield user

  override def validateCredentials(email: String, password: String): Task[Option[User]] =
    for
      user <- userRepository.findByEmail(email)
      isValid <-
        if user.isDefined then checkPassword(password, user.get.passwordHash)
        else ZIO.succeed(false)
      _ <-
        ZIO
          .fail(new Exception("Invalid credentials"))
          .when(!isValid)
    yield user

  override def updateUser(
    id: UserId,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[Option[User]] =
    userRepository.update(id, firstName, lastName)

  override def changePassword(
    id: UserId,
    oldPassword: String,
    newPassword: String,
  ): Task[Boolean] =
    for
      user <- userRepository.findById(id)
      isValid <-
        if user.isDefined then checkPassword(oldPassword, user.get.passwordHash)
        else ZIO.succeed(false)
      _ <-
        ZIO
          .fail(new Exception("Invalid credentials"))
          .when(!isValid)
    yield true

  override def deactivateUser(id: UserId): Task[Boolean] =
    userRepository.deactivate(id)

  private def hashPassword(password: String): Task[String] =
    ZIO.attempt(BCrypt.hashpw(password, BCrypt.gensalt(12)))

  private def checkPassword(password: String, hashedPassword: String): Task[Boolean] =
    ZIO.attempt(BCrypt.checkpw(password, hashedPassword))

object UserServiceImpl:
  val layer: URLayer[UserRepository, UserService] =
    ZLayer:
      for userRepository <- ZIO.service[UserRepository]
      yield new UserServiceImpl(userRepository)
