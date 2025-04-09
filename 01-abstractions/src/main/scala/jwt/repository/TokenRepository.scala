package jwt.repository

import jwt.models.RefreshToken
import zio.*

trait TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): Task[Unit]
  def findByRefreshToken(token: String): Task[Option[RefreshToken]]
  def deleteByRefreshToken(token: String): Task[Unit]
  def deleteAllByUserId(userId: UserId): Task[Unit]
  def cleanExpiredTokens(): Task[Unit]

object TokenRepository:
  def saveRefreshToken(refreshToken: RefreshToken): ZIO[TokenRepository, Nothing, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.saveRefreshToken(refreshToken))

  def findByRefreshToken(token: String): ZIO[TokenRepository, Nothing, Option[RefreshToken]] =
    ZIO.serviceWithZIO[TokenRepository](_.findByRefreshToken(token))

  def deleteByRefreshToken(token: String): ZIO[TokenRepository, Nothing, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteByRefreshToken(token))

  def deleteAllByUserId(userId: UserId): ZIO[TokenRepository, Nothing, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.deleteAllByUserId(userId))

  def cleanExpiredTokens(): ZIO[TokenRepository, Nothing, Unit] =
    ZIO.serviceWithZIO[TokenRepository](_.cleanExpiredTokens())
