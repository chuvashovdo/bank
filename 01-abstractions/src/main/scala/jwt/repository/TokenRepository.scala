package jwt.repository

import jwt.models.RefreshToken
import zio.*
import user.models.UserId
trait TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): Task[Unit]
  def findByRefreshToken(token: String): Task[Option[RefreshToken]]
  def deleteByRefreshToken(token: String): Task[Unit]
  def deleteAllByUserId(userId: UserId): Task[Unit]
  def cleanExpiredTokens(): Task[Unit]

object TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.saveRefreshToken(refreshToken))

  def findByRefreshToken(token: String): RIO[TokenRepository, Option[RefreshToken]] =
    ZIO.serviceWithZIO[TokenRepository](_.findByRefreshToken(token))

  def deleteByRefreshToken(token: String): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteByRefreshToken(token))

  def deleteAllByUserId(userId: UserId): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteAllByUserId(userId))

  def cleanExpiredTokens(): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.cleanExpiredTokens())
