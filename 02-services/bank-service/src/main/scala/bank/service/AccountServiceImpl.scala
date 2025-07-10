package bank.service

import zio.*
import java.time.Instant
import java.sql.SQLException
import java.util.UUID

import bank.models.{ Account, AccountStatus, Balance, AccountId }
import bank.repository.AccountRepository
import user.models.UserId
import bank.errors.{ UnauthorizedAccountAccessError, CannotCloseAccountWithNonZeroBalanceError }
import bank.entity.AccountEntity
import bank.mapper.AccountMapper

private class AccountServiceImpl(
  accountRepository: AccountRepository,
  accountNumberGenerator: AccountNumberGenerator,
) extends AccountService:
  override def createAccount(userId: UserId, currency: String): Task[Account] =
    val createAttempt =
      for
        accountNumber <- accountNumberGenerator.generate
        now = Instant.now()
        accountEntity =
          AccountEntity(
            id = UUID.randomUUID(),
            userId = userId.value,
            accountNumber = accountNumber,
            balance = Balance.zero.value,
            currency = currency.toUpperCase,
            accountStatus = AccountStatus.OPEN,
            createdAt = now,
            updatedAt = now,
          )
        createdEntity <- accountRepository.create(accountEntity)
        createdAccount <- AccountMapper.toModelFromEntity(createdEntity)
      yield createdAccount

    createAttempt.refineToOrDie[SQLException].retry(Schedule.recurs(5)).orDie

  override def getAccount(accountId: AccountId, userId: UserId): Task[Account] =
    for
      accountEntity <- accountRepository.findById(accountId.value)
      account <- AccountMapper.toModelFromEntity(accountEntity)
      _ <-
        ZIO.when(!account.userId.equals(userId)):
          ZIO.fail(UnauthorizedAccountAccessError(userId.value, accountId.value))
    yield account

  override def listAccountsForUser(userId: UserId): Task[List[Account]] =
    accountRepository
      .findByUserId(userId.value)
      .flatMap(ZIO.foreach(_)(AccountMapper.toModelFromEntity))

  override def closeAccount(accountId: AccountId, userId: UserId): Task[Unit] =
    for
      account <- getAccount(accountId, userId)
      _ <-
        ZIO.when(account.balance.value != BigDecimal(0)):
          ZIO.fail(
            CannotCloseAccountWithNonZeroBalanceError(accountId.value, account.balance.value)
          )
      _ <- accountRepository.updateStatus(accountId.value, AccountStatus.CLOSED)
    yield ()

  override def findAllAccounts(): Task[List[Account]] =
    accountRepository.findAll().flatMap(ZIO.foreach(_)(AccountMapper.toModelFromEntity))

  override def getAccountById(accountId: AccountId): Task[Account] =
    accountRepository.findById(accountId.value).flatMap(AccountMapper.toModelFromEntity)

  override def getAccountsByUserId(userId: UserId): Task[List[Account]] =
    accountRepository
      .findByUserId(userId.value)
      .flatMap(ZIO.foreach(_)(AccountMapper.toModelFromEntity))

  override def getAccountByNumber(accountNumber: String): Task[Account] =
    accountRepository.findByAccountNumber(accountNumber).flatMap(AccountMapper.toModelFromEntity)

  override def updateAccountStatus(accountId: AccountId, newStatus: AccountStatus): Task[Unit] =
    accountRepository.updateStatus(accountId.value, newStatus)

  override def deleteAccount(accountId: AccountId): Task[Unit] =
    accountRepository.delete(accountId.value)

object AccountServiceImpl:
  val layer: ZLayer[AccountRepository & AccountNumberGenerator, Nothing, AccountService] =
    ZLayer.fromFunction(new AccountServiceImpl(_, _))
