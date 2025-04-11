package auth.service

import zio.*
import user.service.*
import jwt.service.*
import jwt.repository.*
import user.models.*
import jwt.models.*
import java.time.Instant

final case class AuthServiceImpl(
  userService: UserService,
  tokenRepository: TokenRepository,
  jwtService: JwtService,
) extends AuthService:
  override def login(email: String, password: String): Task[Option[AccessToken]] =
    for
      userOpt <- userService.validateCredentials(email, password)
      tokenOpt <-
        userOpt match
          case Some(user) =>
            jwtService.createAccessToken(user.id, Instant.now()).map(Some(_))
          case None =>
            ZIO.succeed(None)
    yield tokenOpt

  override def register(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[AccessToken] =
    for
      user <- userService.registerUser(email, password, firstName, lastName)
      token <- jwtService.createAccessToken(user.id, Instant.now())
    yield token

  override def validateToken(token: String): Task[Option[UserId]] =
    jwtService.validateToken(token).map(Some(_))

  override def createRefreshToken(userId: UserId): Task[RefreshToken] =
    for
      token <- jwtService.createRefreshToken(userId, Instant.now())
      _ <- tokenRepository.saveRefreshToken(token)
    yield token

  override def refreshToken(refreshToken: String): Task[Option[AccessToken]] =
    for
      refreshTokenOpt <- tokenRepository.findByRefreshToken(refreshToken)
      result <-
        refreshTokenOpt match
          case Some(token) =>
            for
              _ <- tokenRepository.deleteByRefreshToken(refreshToken)
              accessToken <- jwtService.createAccessToken(token.userId, Instant.now())
            yield Some(accessToken)
          case _ =>
            ZIO.succeed(None)
    yield result

  override def logout(userId: UserId): Task[Unit] =
    tokenRepository.deleteAllByUserId(userId)

object AuthServiceImpl:
  val layer: ZLayer[UserService & TokenRepository & JwtService, Nothing, AuthService] =
    ZLayer:
      for
        userService <- ZIO.service[UserService]
        tokenRepository <- ZIO.service[TokenRepository]
        jwtService <- ZIO.service[JwtService]
      yield AuthServiceImpl(userService, tokenRepository, jwtService)
