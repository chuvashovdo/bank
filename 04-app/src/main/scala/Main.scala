package bank

import zio.*
import zio.http.*
import org.flywaydb.core.Flyway
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import common.config.db.{ DbConfig, DbConfigImpl, QuillContext }
import common.service.TransactorImpl
import jwt.config.JwtConfigImpl
import jwt.repository.TokenRepositoryImpl
import jwt.service.{ JwtService, JwtServiceImpl }
import user.repository.UserRepositoryImpl
import user.service.UserServiceImpl
import auth.service.AuthServiceImpl
import bank.repository.{ AccountRepositoryImpl, TransactionRepositoryImpl }
import bank.service.*
import user.api.UserApi
import bank.api.BankApi
import java.net.InetSocketAddress

object Main extends ZIOAppDefault:

  // --- CONFIG LAYERS ---
  private val dbConfigLayer =
    DbConfigImpl.layer
  private val jwtConfigLayer =
    JwtConfigImpl.layer

  // --- INFRASTRUCTURE LAYERS ---
  private val quillLayer =
    dbConfigLayer >>> QuillContext.layer

  private def migrateDb(config: DbConfig): Task[Unit] =
    ZIO.attempt:
      val flyway =
        Flyway
          .configure()
          .dataSource(
            s"jdbc:postgresql://${config.host}:${config.port}/${config.database}",
            config.user,
            config.password,
          )
          .load()
      val _ = flyway.migrate()

  // --- REPOSITORY LAYERS ---
  private val userRepoLayer =
    quillLayer >>> UserRepositoryImpl.layer
  private val tokenRepoLayer =
    quillLayer >>> TokenRepositoryImpl.layer
  private val accountRepoLayer =
    quillLayer >>> AccountRepositoryImpl.layer
  private val transactionRepoLayer =
    quillLayer >>> TransactionRepositoryImpl.layer

  // --- SERVICE LAYERS ---
  private val accountNumberGeneratorLayer =
    AccountNumberGeneratorImpl.layer
  private val transactorLayer =
    quillLayer >>> TransactorImpl.layer

  private val jwtServiceLayer =
    (jwtConfigLayer ++ tokenRepoLayer) >>> JwtServiceImpl.layer
  private val userServiceLayer =
    userRepoLayer >>> UserServiceImpl.layer
  private val authServiceLayer =
    (userServiceLayer ++ jwtServiceLayer) >>> AuthServiceImpl.layer
  private val accountServiceLayer =
    (accountRepoLayer ++ accountNumberGeneratorLayer) >>> AccountServiceImpl.layer
  private val transactionServiceLayer =
    (transactionRepoLayer ++ accountRepoLayer ++ transactorLayer) >>> TransactionServiceImpl.layer

  // --- API LAYERS ---
  private val userApiLayer =
    (authServiceLayer ++ userServiceLayer ++ jwtServiceLayer) >>> UserApi.layer
  private val bankApiLayer =
    (accountServiceLayer ++ transactionServiceLayer ++ jwtServiceLayer) >>> BankApi.layer

  // --- APPLICATION ---
  private val httpApp =
    for
      userApi <- ZIO.service[UserApi]
      bankApi <- ZIO.service[BankApi]
      apiEndpoints = userApi.apiEndpoints ++ bankApi.allEndpoints
      swaggerEndpoints =
        SwaggerInterpreter()
          .fromServerEndpoints[Task](
            apiEndpoints,
            "Bank API",
            "1.0.0",
          )
    yield ZioHttpInterpreter().toHttp(apiEndpoints ++ swaggerEndpoints)

  private val program =
    for
      _ <- Console.printLine("Starting Bank API...")
      config <- ZIO.service[DbConfig]
      _ <- migrateDb(config).orDie
      _ <- Console.printLine("Database migration complete")

      serverPort = 8080
      serverHost = "0.0.0.0"

      _ <- Console.printLine(s"API docs available at: http://localhost:$serverPort/docs")

      routes <- httpApp
      serverConfig = Server.Config.default.binding(InetSocketAddress(serverHost, serverPort))

      _ <- Server.serve(routes).provide(ZLayer.succeed(serverConfig), Server.live)
    yield ()

  private val appLayer =
    userApiLayer ++
      bankApiLayer ++
      dbConfigLayer

  override def run: ZIO[Any, Any, Any] =
    program.provide(appLayer)
