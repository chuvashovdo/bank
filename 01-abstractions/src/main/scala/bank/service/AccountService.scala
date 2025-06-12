package bank.service

import zio.*
import java.util.UUID
import bank.models.Account
import user.models.UserId

trait AccountService:
  def createAccount(userId: UserId, currency: String): Task[Account]
  def getAccount(accountId: UUID, userId: UserId): Task[Account]
  def listAccountsForUser(userId: UserId): Task[List[Account]]
  def closeAccount(accountId: UUID, userId: UserId): Task[Unit]

object AccountService:
  def createAccount(userId: UserId, currency: String): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.createAccount(userId, currency))

  def getAccount(accountId: UUID, userId: UserId): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.getAccount(accountId, userId))

  def listAccountsForUser(userId: UserId): RIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO[AccountService](_.listAccountsForUser(userId))

  def closeAccount(accountId: UUID, userId: UserId): RIO[AccountService, Unit] =
    ZIO.serviceWithZIO[AccountService](_.closeAccount(accountId, userId))
