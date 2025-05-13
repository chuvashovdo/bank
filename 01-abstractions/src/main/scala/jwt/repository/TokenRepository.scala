package jwt.repository

import jwt.models.RefreshToken
import zio.*
import user.models.UserId
import jwt.models.JwtRefreshToken

trait TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): Task[Unit]
  def findByRefreshToken(token: JwtRefreshToken): Task[Option[RefreshToken]]
  def deleteByRefreshToken(token: JwtRefreshToken): Task[Unit]
  def deleteAllByUserId(userId: UserId): Task[Unit]
  def cleanExpiredTokens(): Task[Unit]

object TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.saveRefreshToken(refreshToken))

  def findByRefreshToken(token: JwtRefreshToken): RIO[TokenRepository, Option[RefreshToken]] =
    ZIO.serviceWithZIO[TokenRepository](_.findByRefreshToken(token))

  def deleteByRefreshToken(token: JwtRefreshToken): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteByRefreshToken(token))

  def deleteAllByUserId(userId: UserId): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteAllByUserId(userId))

  def cleanExpiredTokens(): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.cleanExpiredTokens())
