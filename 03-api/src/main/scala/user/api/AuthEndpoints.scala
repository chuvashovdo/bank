package user.api

import auth.service.*
import common.models.ErrorResponse
import common.TapirSchemas.given
import jwt.service.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import user.models.*
import user.models.{ RegisterUserRequest, LoginRequest, AuthResponse, RefreshTokenRequest }
import user.service.*
import zio.*
import java.time.Instant
import sttp.tapir.server.ServerEndpoint

/** Объект-компаньон для хранения констант путей */
object AuthEndpoints:
  private val registerPath =
    "/api/users"
  private val loginPath =
    "/api/auth/token"
  private val refreshPath =
    "/api/auth/token/refresh"
  private val logoutPath =
    "/api/auth/token"

/** Эндпоинты аутентификации и регистрации пользователей */
class AuthEndpoints(
  authService: AuthService,
  jwtService: JwtService,
  userService: UserService,
) extends ApiEndpoint:
  import AuthEndpoints.* // Импортируем пути из объекта-компаньона

  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  val registerEndpoint: ServerEndpoint[Any, Task] =
    baseEndpoint
      .post
      .in("api" / "users")
      .summary("Регистрация нового пользователя")
      .in(jsonBody[RegisterUserRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        handleRegister(request).either
      }

  val loginEndpoint: ServerEndpoint[Any, Task] =
    baseEndpoint
      .post
      .in("api" / "auth" / "token")
      .summary("Аутентификация пользователя и получение токенов")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        handleLogin(request).either
      }

  val refreshTokenEndpoint: ServerEndpoint[Any, Task] =
    baseEndpoint
      .post
      .in("api" / "auth" / "token" / "refresh")
      .summary("Обновление access токена с использованием refresh токена")
      .in(jsonBody[RefreshTokenRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        handleRefreshToken(request).either
      }

  val logoutEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .delete
      .in("api" / "auth" / "token")
      .summary("Выход пользователя из системы (удаление сессии/токена)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => _ =>
        handleLogout(userId)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      registerEndpoint,
      loginEndpoint,
      refreshTokenEndpoint,
      logoutEndpoint,
    )

  // --- Private handler methods for endpoint logic ---

  private def findAndMapUserToResponse(
    userId: UserId,
    path: String,
  ): ZIO[Any, ErrorResponse, UserResponse] =
    userService
      .findUserById(userId)
      .mapError(handleCommonErrors(path))
      .map(user => UserResponse(user.id, user.email, user.firstName, user.lastName))

  private def handleRegister(request: RegisterUserRequest): ZIO[Any, ErrorResponse, AuthResponse] =
    for
      accessToken <-
        authService
          .register(
            request.email,
            request.password,
            request.firstName,
            request.lastName,
          )
          .mapError(handleCommonErrors(registerPath))

      userResponse <- findAndMapUserToResponse(accessToken.userId, registerPath)

      refreshToken <-
        jwtService
          .createRefreshToken(accessToken.userId, Instant.now())
          .mapError(handleCommonErrors(registerPath))
    yield AuthResponse(
      accessToken = accessToken.token,
      refreshToken = refreshToken.token,
      expiresAt = accessToken.expiresAt.toEpochMilli,
      user = userResponse,
    )

  private def handleLogin(request: LoginRequest): ZIO[Any, ErrorResponse, AuthResponse] =
    for
      accessToken <-
        authService
          .login(request.email, request.password)
          .mapError(handleCommonErrors(loginPath))

      userResponse <- findAndMapUserToResponse(accessToken.userId, loginPath)

      refreshToken <-
        jwtService
          .createRefreshToken(accessToken.userId, Instant.now())
          .mapError(handleCommonErrors(loginPath))
    yield AuthResponse(
      accessToken = accessToken.token,
      refreshToken = refreshToken.token,
      expiresAt = accessToken.expiresAt.toEpochMilli,
      user = userResponse,
    )

  private def handleRefreshToken(
    request: RefreshTokenRequest
  ): ZIO[Any, ErrorResponse, AuthResponse] =
    for
      newAccessToken <-
        jwtService
          .renewAccessToken(request.refreshToken)
          .mapError(handleCommonErrors(refreshPath))

      userResponse <- findAndMapUserToResponse(newAccessToken.userId, refreshPath)

      newRefreshToken <-
        jwtService
          .createRefreshToken(newAccessToken.userId, Instant.now())
          .mapError(handleCommonErrors(refreshPath))
    yield AuthResponse(
      accessToken = newAccessToken.token,
      refreshToken = newRefreshToken.token,
      expiresAt = newAccessToken.expiresAt.toEpochMilli,
      user = userResponse,
    )

  private def handleLogout(userId: UserId): ZIO[Any, ErrorResponse, Unit] =
    authService
      .logout(userId)
      .mapError(handleCommonErrors(logoutPath))
