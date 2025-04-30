package jwt.service

import jwt.models.*
import user.models.UserId
import jwt.entity.RefreshTokenEntity
import java.time.Instant
import zio.*

trait JwtService:
  def createAccessToken(userId: UserId, issuedAt: Instant): Task[AccessToken]
  def createRefreshToken(userId: UserId, issuedAt: Instant): Task[RefreshToken]
  def validateToken(token: String): Task[UserId]
  def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: String,
    expiresAt: Instant,
  ): Task[RefreshTokenEntity]

object JwtService:
  def createAccessToken(userId: UserId, issuedAt: Instant): RIO[JwtService, AccessToken] =
    ZIO.serviceWithZIO[JwtService](_.createAccessToken(userId, issuedAt))
  def createRefreshToken(userId: UserId, issuedAt: Instant): RIO[JwtService, RefreshToken] =
    ZIO.serviceWithZIO[JwtService](_.createRefreshToken(userId, issuedAt))
  def validateToken(token: String): RIO[JwtService, UserId] =
    ZIO.serviceWithZIO[JwtService](_.validateToken(token))
  def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: String,
    expiresAt: Instant,
  ): RIO[JwtService, RefreshTokenEntity] =
    ZIO.serviceWithZIO[JwtService](
      _.createRefreshTokenEntity(id, userId, refreshToken, expiresAt)
    )
