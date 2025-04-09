package jwt.config

import zio.*

trait JwtConfig:
  def secretKey: Task[String]
  def accessTokenExpiration: Task[Long]
  def refreshTokenExpiration: Task[Long]
  def issuer: Task[String]
  def audience: Task[String]
  def accessTokenExpirationMillis: Task[Long]
  def refreshTokenExpirationMillis: Task[Long]

object JwtConfig:
  def secretKey: RIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.secretKey)
  def accessTokenExpiration: RIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.accessTokenExpiration)
  def refreshTokenExpiration: RIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.refreshTokenExpiration)
  def issuer: RIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.issuer)
  def audience: RIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.audience)
  def accessTokenExpirationMillis: RIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.accessTokenExpirationMillis)
  def refreshTokenExpirationMillis: RIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.refreshTokenExpirationMillis)
