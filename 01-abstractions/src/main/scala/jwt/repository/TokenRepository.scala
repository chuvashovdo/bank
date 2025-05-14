package jwt.repository

import jwt.entity.RefreshTokenEntity
import jwt.models.RefreshToken
import zio.*

trait TokenRepository:
  def saveRefreshToken(tokenEntity: RefreshTokenEntity): Task[Unit]
  def findByRefreshToken(token: String): Task[RefreshToken]
  def deleteByRefreshToken(token: String): Task[Unit]
  def deleteAllByUserId(userId: String): Task[Unit]
  def cleanExpiredTokens(): Task[Unit]

object TokenRepository:
  def saveRefreshToken(tokenEntity: RefreshTokenEntity): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.saveRefreshToken(tokenEntity))

  def findByRefreshToken(token: String): RIO[TokenRepository, RefreshToken] =
    ZIO.serviceWithZIO[TokenRepository](_.findByRefreshToken(token))

  def deleteByRefreshToken(token: String): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteByRefreshToken(token))

  def deleteAllByUserId(userId: String): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteAllByUserId(userId))

  def cleanExpiredTokens(): RIO[TokenRepository, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.cleanExpiredTokens())
