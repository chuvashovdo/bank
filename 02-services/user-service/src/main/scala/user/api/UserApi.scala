package user.api

import auth.service.*
import user.service.*
import user.models.*
import user.mapper.*
import sttp.model.StatusCode
import zio.*
// Tapir imports
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto.*

class UserApi(
  authService: AuthService,
  userService: UserService,
  userResponseMapper: UserResponseMapper,
):

  // Базовые эндпоинты
  private val baseEndpoint =
    endpoint.errorOut:
      oneOf[ErrorResponse](
        sttp.tapir.oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]),
        sttp.tapir.oneOfVariant(StatusCode.Unauthorized, jsonBody[ErrorResponse]),
        sttp.tapir.oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse]),
        sttp.tapir.oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse]),
      )

  private val securedEndpoint =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .serverSecurityLogic { token =>
        authService
          .validateToken(token)
          .map:
            case Some(userId) => Right(userId)
            case None => Left(ErrorResponse("Invalid or expired token"))
      }

  // Публичные эндпоинты
  val registerEndpoint =
    baseEndpoint
      .post
      .in("api" / "auth" / "register")
      .in(jsonBody[RegisterUserRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        val register =
          for
            accessToken <-
              authService.register(
                request.email,
                request.password,
                request.firstName,
                request.lastName,
              )
            userOpt <- userService.findUserById(accessToken.userId)
            user <-
              ZIO
                .fromOption(userOpt)
                .orElseFail(
                  ErrorResponse("User not found after registration")
                )
            // Генерируем refresh token отдельно
            refreshToken <- authService.createRefreshToken(accessToken.userId)
            userResponse <- userResponseMapper.fromUser(user)
          yield AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            expiresAt = accessToken.expiresAt.toEpochMilli,
            user = userResponse,
          )

        register
          .either
          .map:
            case Right(response) => Right(response)
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val loginEndpoint =
    baseEndpoint
      .post
      .in("api" / "auth" / "login")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        val login =
          for
            accessTokenOpt <- authService.login(request.email, request.password)
            accessToken <-
              ZIO
                .fromOption(accessTokenOpt)
                .orElseFail(
                  ErrorResponse("Invalid credentials")
                )
            userOpt <- userService.findUserById(accessToken.userId)
            user <-
              ZIO
                .fromOption(userOpt)
                .orElseFail(
                  ErrorResponse("User not found")
                )
            // Генерируем refresh token отдельно
            refreshToken <- authService.createRefreshToken(accessToken.userId)
            userResponse <- userResponseMapper.fromUser(user)
          yield AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            expiresAt = accessToken.expiresAt.toEpochMilli,
            user = userResponse,
          )

        login
          .either
          .map:
            case Right(response) => Right(response)
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val refreshTokenEndpoint =
    baseEndpoint
      .post
      .in("api" / "auth" / "refresh")
      .in(jsonBody[RefreshTokenRequest])
      .out(jsonBody[AuthResponse])
      .serverLogic { request =>
        val refresh =
          for
            accessTokenOpt <- authService.refreshToken(request.refreshToken)
            accessToken <-
              ZIO
                .fromOption(accessTokenOpt)
                .orElseFail(
                  ErrorResponse("Invalid or expired refresh token")
                )
            userOpt <- userService.findUserById(accessToken.userId)
            user <-
              ZIO
                .fromOption(userOpt)
                .orElseFail(
                  ErrorResponse("User not found")
                )
            // Генерируем новый refresh token
            refreshToken <- authService.createRefreshToken(accessToken.userId)
            userResponse <- userResponseMapper.fromUser(user)
          yield AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            expiresAt = accessToken.expiresAt.toEpochMilli,
            user = userResponse,
          )

        refresh
          .either
          .map:
            case Right(response) => Right(response)
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val logoutEndpoint =
    securedEndpoint
      .post
      .in("api" / "auth" / "logout")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => _ =>
        authService
          .logout(userId)
          .either
          .map:
            case Right(_) => Right(())
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  // Защищенные эндпоинты
  val getCurrentUserEndpoint =
    securedEndpoint
      .get
      .in("api" / "users" / "me")
      .out(jsonBody[UserResponse])
      .serverLogic { userId => _ =>
        val getUser =
          for
            userOpt <- userService.findUserById(userId)
            user <-
              ZIO
                .fromOption(userOpt)
                .orElseFail(
                  ErrorResponse("User not found")
                )
            userResponse <- userResponseMapper.fromUser(user)
          yield userResponse

        getUser
          .either
          .map:
            case Right(response) => Right(response)
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val updateUserEndpoint =
    securedEndpoint
      .put
      .in("api" / "users" / "me")
      .in(jsonBody[UpdateUserRequest])
      .out(jsonBody[UserResponse])
      .serverLogic { userId => request =>
        val updateUser =
          for
            userOpt <- userService.updateUser(userId, request.firstName, request.lastName)
            user <-
              ZIO
                .fromOption(userOpt)
                .orElseFail(
                  ErrorResponse("User not found")
                )
            userResponse <- userResponseMapper.fromUser(user)
          yield userResponse

        updateUser
          .either
          .map:
            case Right(response) => Right(response)
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val changePasswordEndpoint =
    securedEndpoint
      .post
      .in("api" / "users" / "change-password")
      .in(jsonBody[ChangePasswordRequest])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => request =>
        val changePassword =
          for
            success <- userService.changePassword(userId, request.oldPassword, request.newPassword)
            _ <-
              ZIO.unless(success)(
                ZIO.fail(ErrorResponse("Invalid old password"))
              )
          yield ()

        changePassword
          .either
          .map:
            case Right(_) => Right(())
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val deactivateAccountEndpoint =
    securedEndpoint
      .delete
      .in("api" / "users" / "me")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { userId => _ =>
        val deactivate =
          for
            success <- userService.deactivateUser(userId)
            _ <-
              ZIO.unless(success)(
                ZIO.fail(ErrorResponse("Failed to deactivate account"))
              )
            _ <- authService.logout(userId)
          yield ()

        deactivate
          .either
          .map:
            case Right(_) => Right(())
            case Left(e) => Left(ErrorResponse(e.toString))
      }

  val apiEndpoints: List[ServerEndpoint[Any, Task]] =
    List[ServerEndpoint[Any, Task]](
      registerEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      loginEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      refreshTokenEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      logoutEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      getCurrentUserEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      updateUserEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      changePasswordEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
      deactivateAccountEndpoint.asInstanceOf[ServerEndpoint[Any, Task]],
    )

  // Swagger UI
  val swaggerEndpoints =
    SwaggerInterpreter()
      .fromServerEndpoints[Task](
        apiEndpoints,
        "Bank API",
        "1.0.0",
      )

  val allEndpoints =
    apiEndpoints ++ swaggerEndpoints

  val routes =
    ZioHttpInterpreter().toHttp(allEndpoints)

object UserApi:
  val layer: URLayer[AuthService & UserService & UserResponseMapper, UserApi] =
    ZLayer:
      for
        authService <- ZIO.service[AuthService]
        userService <- ZIO.service[UserService]
        userResponseMapper <- ZIO.service[UserResponseMapper]
      yield new UserApi(authService, userService, userResponseMapper)
