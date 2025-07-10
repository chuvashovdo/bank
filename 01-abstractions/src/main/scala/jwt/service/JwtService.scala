package jwt.service

import zio.*
import jwt.models.{ AccessToken, RefreshToken, JwtAccessToken, JwtRefreshToken, AuthContext }
import user.models.{ User, UserId }
import java.time.Instant

trait JwtService:
  def createAccessToken(user: User, issuedAt: Instant = Instant.now()): Task[AccessToken]
  def createRefreshToken(
    userId: UserId,
    issuedAt: Instant = Instant.now(),
  ): Task[RefreshToken]
  def validateToken(token: JwtAccessToken): Task[AuthContext]
  def renewAccessToken(token: JwtRefreshToken): Task[AccessToken]
  def invalidateRefreshTokens(userId: UserId): Task[Unit]

object JwtService:
  def createAccessToken(
    user: User,
    issuedAt: Instant,
  ): RIO[JwtService, AccessToken] =
    ZIO.serviceWithZIO[JwtService](_.createAccessToken(user, issuedAt))
  def createRefreshToken(
    userId: UserId,
    issuedAt: Instant,
  ): RIO[JwtService, RefreshToken] =
    ZIO.serviceWithZIO[JwtService](_.createRefreshToken(userId, issuedAt))
  def validateToken(token: JwtAccessToken): RIO[JwtService, AuthContext] =
    ZIO.serviceWithZIO[JwtService](_.validateToken(token))
  def renewAccessToken(token: JwtRefreshToken): RIO[JwtService, AccessToken] =
    ZIO.serviceWithZIO[JwtService](_.renewAccessToken(token))
  def invalidateRefreshTokens(userId: UserId): RIO[JwtService, Unit] =
    ZIO.serviceWithZIO[JwtService](_.invalidateRefreshTokens(userId))
