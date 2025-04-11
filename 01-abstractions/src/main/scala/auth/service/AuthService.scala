package auth.service

import zio.*
import jwt.models.*
import user.models.UserId

trait AuthService:
  def login(email: String, password: String): Task[Option[AccessToken]]
  def register(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): Task[AccessToken]
  def validateToken(token: String): Task[Option[UserId]]
  def createRefreshToken(userId: UserId): Task[RefreshToken]
  def refreshToken(refreshToken: String): Task[Option[AccessToken]]
  def logout(userId: UserId): Task[Unit]

object AuthService:
  def login(email: String, password: String): RIO[AuthService, Option[AccessToken]] =
    ZIO.serviceWithZIO[AuthService](_.login(email, password))
  def register(
    email: String,
    password: String,
    firstName: Option[String],
    lastName: Option[String],
  ): RIO[AuthService, AccessToken] =
    ZIO.serviceWithZIO[AuthService](_.register(email, password, firstName, lastName))
  def validateToken(token: String): RIO[AuthService, Option[UserId]] =
    ZIO.serviceWithZIO[AuthService](_.validateToken(token))
  def createRefreshToken(userId: UserId): RIO[AuthService, RefreshToken] =
    ZIO.serviceWithZIO[AuthService](_.createRefreshToken(userId))
  def refreshToken(refreshToken: String): RIO[AuthService, Option[AccessToken]] =
    ZIO.serviceWithZIO[AuthService](_.refreshToken(refreshToken))
  def logout(userId: UserId): RIO[AuthService, Unit] =
    ZIO.serviceWithZIO[AuthService](_.logout(userId))
