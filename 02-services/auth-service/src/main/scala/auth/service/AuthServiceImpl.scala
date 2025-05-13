package auth.service

import zio.*
import user.service.UserService
import jwt.service.JwtService
import user.models.{ Email, Password, FirstName, LastName, User, UserId }
import jwt.models.AccessToken
import java.time.Instant

final case class AuthServiceImpl(
  userService: UserService,
  jwtService: JwtService,
) extends AuthService:
  override def login(email: Email, password: Password): Task[Option[AccessToken]] =
    for
      userOpt: Option[User] <- userService.validateCredentials(email, password)
      tokenOpt <-
        userOpt match
          case Some(user) =>
            jwtService.createAccessToken(user.id, Instant.now()).map(Some(_))
          case None =>
            ZIO.succeed(None)
    yield tokenOpt

  override def register(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[AccessToken] =
    for
      user <-
        userService.registerUser(
          email,
          password,
          firstName,
          lastName,
        )
      token <- jwtService.createAccessToken(user.id, Instant.now())
    yield token

  override def logout(userId: UserId): Task[Unit] =
    jwtService.invalidateRefreshTokens(userId)

object AuthServiceImpl:
  val layer: ZLayer[UserService & JwtService, Nothing, AuthService] =
    ZLayer:
      for
        userService <- ZIO.service[UserService]
        jwtService <- ZIO.service[JwtService]
      yield AuthServiceImpl(userService, jwtService)
