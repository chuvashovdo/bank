package jwt.service

import jwt.models.*
import user.models.UserId
import java.time.Instant
import zio.*

trait JwtService:
  def createAccessToken(userId: UserId, issuedAt: Instant): Task[AccessToken]
  def createRefreshToken(userId: UserId, issuedAt: Instant): Task[RefreshToken]
  def validateToken(token: String): Task[UserId]
  def refreshToken(token: String): Task[Option[AccessToken]]
  def invalidateRefreshTokens(userId: UserId): Task[Unit]

object JwtService:
  def createAccessToken(userId: UserId, issuedAt: Instant): RIO[JwtService, AccessToken] =
    ZIO.serviceWithZIO[JwtService](_.createAccessToken(userId, issuedAt))
  def createRefreshToken(userId: UserId, issuedAt: Instant): RIO[JwtService, RefreshToken] =
    ZIO.serviceWithZIO[JwtService](_.createRefreshToken(userId, issuedAt))
  def validateToken(token: String): RIO[JwtService, UserId] =
    ZIO.serviceWithZIO[JwtService](_.validateToken(token))
  def refreshToken(token: String): RIO[JwtService, Option[AccessToken]] =
    ZIO.serviceWithZIO[JwtService](_.refreshToken(token))
  def invalidateRefreshTokens(userId: UserId): RIO[JwtService, Unit] =
    ZIO.serviceWithZIO[JwtService](_.invalidateRefreshTokens(userId))
