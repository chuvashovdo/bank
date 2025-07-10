package bank.service

import zio.*
import bank.models.Account
import user.models.UserId
import bank.models.AccountId

trait AccountService:
  def createAccount(userId: UserId, currency: String): Task[Account]
  def getAccount(accountId: AccountId, userId: UserId): Task[Account]
  def listAccountsForUser(userId: UserId): Task[List[Account]]
  def closeAccount(accountId: AccountId, userId: UserId): Task[Unit]

  def findAllAccounts(): Task[List[Account]]
  def getAccountById(accountId: AccountId): Task[Account]
  def getAccountsByUserId(userId: UserId): Task[List[Account]]
  def getAccountByNumber(accountNumber: String): Task[Account]
  def updateAccountStatus(accountId: AccountId, newStatus: bank.models.AccountStatus): Task[Unit]
  def deleteAccount(accountId: AccountId): Task[Unit]

object AccountService:
  def createAccount(userId: UserId, currency: String): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.createAccount(userId, currency))

  def getAccount(accountId: AccountId, userId: UserId): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.getAccount(accountId, userId))

  def listAccountsForUser(userId: UserId): RIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO[AccountService](_.listAccountsForUser(userId))

  def closeAccount(accountId: AccountId, userId: UserId): RIO[AccountService, Unit] =
    ZIO.serviceWithZIO[AccountService](_.closeAccount(accountId, userId))

  def findAllAccounts(): RIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO[AccountService](_.findAllAccounts())

  def getAccountById(accountId: AccountId): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.getAccountById(accountId))

  def getAccountsByUserId(userId: UserId): RIO[AccountService, List[Account]] =
    ZIO.serviceWithZIO[AccountService](_.getAccountsByUserId(userId))

  def getAccountByNumber(accountNumber: String): RIO[AccountService, Account] =
    ZIO.serviceWithZIO[AccountService](_.getAccountByNumber(accountNumber))

  def updateAccountStatus(
    accountId: AccountId,
    newStatus: bank.models.AccountStatus,
  ): RIO[AccountService, Unit] =
    ZIO.serviceWithZIO[AccountService](_.updateAccountStatus(accountId, newStatus))

  def deleteAccount(accountId: AccountId): RIO[AccountService, Unit] =
    ZIO.serviceWithZIO[AccountService](_.deleteAccount(accountId))
