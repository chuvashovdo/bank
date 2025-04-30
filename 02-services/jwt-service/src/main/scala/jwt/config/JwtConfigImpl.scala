package jwt.config

import zio.*
import scala.util.Properties

class JwtConfigImpl extends JwtConfig:
  override def secretKey: Task[String] =
    JwtConfigImpl.getEnvOrDefault("JWT_SECRET_KEY", "default-secure-key-30chars-minimum-default")

  override def accessTokenExpiration: Task[Long] =
    JwtConfigImpl.getEnvOrDefaultLong("JWT_ACCESS_TOKEN_EXPIRATION_MINUTES", 60)

  override def refreshTokenExpiration: Task[Long] =
    JwtConfigImpl.getEnvOrDefaultLong("JWT_REFRESH_TOKEN_EXPIRATION_DAYS", 30)

  override def issuer: Task[String] =
    JwtConfigImpl.getEnvOrDefault("JWT_ISSUER", "bank-auth-service")

  override def audience: Task[String] =
    JwtConfigImpl.getEnvOrDefault("JWT_AUDIENCE", "bank-api")

  override def accessTokenExpirationMillis: Task[Long] =
    accessTokenExpiration.map(_ * 60 * 1000)

  override def refreshTokenExpirationMillis: Task[Long] =
    refreshTokenExpiration.map(_ * 24 * 60 * 60 * 1000)

object JwtConfigImpl:
  private def getEnvOrDefault(name: String, default: String): Task[String] =
    ZIO.attempt(Properties.envOrElse(name, default))

  private def getEnvOrDefaultLong(name: String, default: Long): Task[Long] =
    ZIO.attempt(Properties.envOrElse(name, default.toString).toLong)

  val layer: ULayer[JwtConfig] =
    ZLayer.succeed(new JwtConfigImpl)
