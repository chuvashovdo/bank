package bank

import zio.*
import org.flywaydb.core.Flyway
import io.getquill.*
import zio.http.*
import scala.language.unsafeNulls
import jwt.config.{ JwtConfig, JwtConfigImpl }
import jwt.service.{ JwtService, JwtServiceImpl }
import user.service.{ UserService, UserServiceImpl }
import common.db.{ DbConfig, DbConfigImpl, QuillContext }
import jwt.repository.{ TokenRepository, TokenRepositoryImpl }
import auth.service.*
import user.repository.*
import user.api.*
import bank.service.*
import bank.repository.*
import bank.api.*
import common.service.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import java.nio.file.{ Files, Paths }
import scala.jdk.CollectionConverters.*
import java.net.InetSocketAddress

object Main extends ZIOAppDefault:
  // Загрузка переменных окружения из .env файла
  private def loadEnv(): Unit =
    val envPath = Paths.get(".env")
    if Files.exists(envPath) then
      val lines = Files.readAllLines(envPath).asScala

      for line <- lines do
        if !line.startsWith("#") && line.contains("=") then
          val parts = line.split("=", 2)
          if parts.length == 2 then
            val key = parts(0).trim
            val value = parts(1).trim
            val _ = java.lang.System.setProperty(key, value)

  loadEnv()

  val serverPort =
    8080
  val serverHost =
    "0.0.0.0" // Слушаем на всех интерфейсах

  val migrateDb: ZIO[DbConfig, Throwable, Unit] =
    ZIO.service[DbConfig].flatMap { config =>
      ZIO.attempt:
        val flyway =
          Flyway
            .configure()
            .dataSource(config.url, config.user, config.password)
            .load()
        val _ = flyway.migrate()
    }

  val jwtConfigLayer =
    JwtConfigImpl.layer

  val tokenRepositoryLayer =
    ZLayer.make[TokenRepository](
      TokenRepositoryImpl.layer,
      DbConfigImpl.layer,
      QuillContext.dataSourceLayer,
    )

  val jwtServiceLayer =
    ZLayer.make[JwtService](
      JwtServiceImpl.layer,
      tokenRepositoryLayer,
      jwtConfigLayer,
    )

  val userServiceLayer =
    ZLayer.make[UserService](
      UserServiceImpl.layer,
      UserRepositoryImpl.layer,
      DbConfigImpl.layer,
      QuillContext.dataSourceLayer,
    )

  val authServiceLayer =
    ZLayer.make[AuthService](
      AuthServiceImpl.layer,
      jwtServiceLayer,
      userServiceLayer,
    )

  val accountServiceLayer =
    ZLayer.make[AccountService](
      AccountServiceImpl.layer,
      AccountRepositoryImpl.layer,
      AccountNumberGeneratorImpl.layer,
      DbConfigImpl.layer,
      QuillContext.dataSourceLayer,
    )

  val transactionServiceLayer =
    ZLayer.make[TransactionService](
      TransactionServiceImpl.layer,
      TransactionRepositoryImpl.layer,
      AccountRepositoryImpl.layer,
      TransactorImpl.layer,
      DbConfigImpl.layer,
      QuillContext.dataSourceLayer,
    )

  val bankApiLayer =
    ZLayer.make[BankApi](
      BankApi.layer,
      accountServiceLayer,
      transactionServiceLayer,
      jwtServiceLayer,
    )

  val userApiLayer =
    ZLayer.make[UserApi](
      UserApi.layer,
      jwtServiceLayer,
      authServiceLayer,
      userServiceLayer,
    )

  val program =
    for
      _ <- Console.printLine("Starting Bank API...")
      _ <- migrateDb
      _ <- Console.printLine("Database migration complete")
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
      allRoutes = ZioHttpInterpreter().toHttp(apiEndpoints ++ swaggerEndpoints)
      _ <- Console.printLine(s"API docs available at: http://localhost:$serverPort/docs")
      config =
        Server.Config.default.port(serverPort).binding(InetSocketAddress(serverHost, serverPort))
      server <-
        Server
          .serve(allRoutes)
          .provide(
            ZLayer.succeed(config),
            Server.live,
          )
          .fork
      _ <- ZIO.never
    yield ()

  override def run =
    program.provide(
      userApiLayer,
      bankApiLayer,
      DbConfigImpl.layer,
    )
