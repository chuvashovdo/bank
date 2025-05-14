package auth.service

import zio.*
import jwt.models.AccessToken
import user.models.{ Email, FirstName, LastName, Password, UserId }

trait AuthService:
  def login(email: Email, password: Password): Task[AccessToken]
  def register(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): Task[AccessToken]
  def logout(userId: UserId): Task[Unit]

object AuthService:
  def login(email: Email, password: Password): RIO[AuthService, AccessToken] =
    ZIO.serviceWithZIO[AuthService](_.login(email, password))
  def register(
    email: Email,
    password: Password,
    firstName: Option[FirstName],
    lastName: Option[LastName],
  ): RIO[AuthService, AccessToken] =
    ZIO.serviceWithZIO[AuthService](_.register(email, password, firstName, lastName))
  def logout(userId: UserId): RIO[AuthService, Unit] =
    ZIO.serviceWithZIO[AuthService](_.logout(userId))
