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
  def secretKey: URIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.secretKey)
  def accessTokenExpiration: URIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.accessTokenExpiration)
  def refreshTokenExpiration: URIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.refreshTokenExpiration)
  def issuer: URIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.issuer)
  def audience: URIO[JwtConfig, String] =
    ZIO.serviceWithZIO[JwtConfig](_.audience)
  def accessTokenExpirationMillis: URIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.accessTokenExpirationMillis)
  def refreshTokenExpirationMillis: URIO[JwtConfig, Long] =
    ZIO.serviceWithZIO[JwtConfig](_.refreshTokenExpirationMillis)
