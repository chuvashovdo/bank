package user.api

import auth.service.*
import jwt.service.*
import zio.*
import sttp.tapir.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ServerEndpoint
import user.service.*

class UserApi(
  authService: AuthService,
  jwtService: JwtService,
  userService: UserService,
):
  private val authEndpoints =
    new AuthEndpoints(
      authService,
      jwtService,
      userService,
    )

  private val userAccountEndpoints =
    new UserAccountEndpoints(
      userService,
      authService,
      jwtService,
    )

  val apiEndpoints: List[ServerEndpoint[Any, Task]] =
    authEndpoints.all ++ userAccountEndpoints.all

  val swaggerEndpoints: List[ServerEndpoint[Any, Task]] =
    SwaggerInterpreter()
      .fromServerEndpoints[Task](
        apiEndpoints,
        "Bank API",
        "1.0.0",
      )

  val allEndpoints: List[ServerEndpoint[Any, Task]] =
    apiEndpoints ++ swaggerEndpoints

  val routes =
    ZioHttpInterpreter().toHttp(allEndpoints)

object UserApi:
  val layer: URLayer[AuthService & JwtService & UserService, UserApi] =
    ZLayer:
      for
        authService <- ZIO.service[AuthService]
        jwtService <- ZIO.service[JwtService]
        userService <- ZIO.service[UserService]
      yield new UserApi(authService, jwtService, userService)
