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
  def logout(userId: UserId): RIO[AuthService, Unit] =
    ZIO.serviceWithZIO[AuthService](_.logout(userId))
