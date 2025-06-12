package bank.service

import zio.*
import java.util.UUID
import java.time.Instant
import java.sql.SQLException

import bank.models.{ Account, AccountStatus, Balance }
import bank.repository.AccountRepository
import user.models.UserId
import bank.errors.{ UnauthorizedAccountAccessError, CannotCloseAccountWithNonZeroBalanceError }

private class AccountServiceImpl(
  accountRepository: AccountRepository,
  accountNumberGenerator: AccountNumberGenerator,
) extends AccountService:
  override def createAccount(userId: UserId, currency: String): Task[Account] =
    val createAttempt =
      for
        accountNumber <- accountNumberGenerator.generate
        now <- ZIO.succeed(Instant.now())
        account =
          Account(
            id = UUID.randomUUID(),
            userId = userId,
            accountNumber = accountNumber,
            balance = Balance.zero,
            currency = currency.toUpperCase,
            accountStatus = AccountStatus.OPEN,
            createdAt = now,
            updatedAt = now,
          )
        createdAccount <- accountRepository.create(account)
      yield createdAccount

    createAttempt.retry(Schedule.recurs(5) && Schedule.recurWhile {
      case e: SQLException => true
      case _ => false
    })

  override def getAccount(accountId: UUID, userId: UserId): Task[Account] =
    for
      account <- accountRepository.findById(accountId)
      _ <-
        ZIO.when(account.userId != userId):
          ZIO.fail(UnauthorizedAccountAccessError(userId.value, accountId))
    yield account

  override def listAccountsForUser(userId: UserId): Task[List[Account]] =
    accountRepository.findByUserId(userId.value)

  override def closeAccount(accountId: UUID, userId: UserId): Task[Unit] =
    for
      account <- getAccount(accountId, userId)
      _ <-
        ZIO.when(account.balance.value != BigDecimal(0)):
          ZIO.fail(CannotCloseAccountWithNonZeroBalanceError(accountId, account.balance.value))
      _ <- accountRepository.updateStatus(accountId, AccountStatus.CLOSED)
    yield ()

object AccountServiceImpl:
  val layer: ZLayer[AccountRepository & AccountNumberGenerator, Nothing, AccountService] =
    ZLayer.fromFunction(new AccountServiceImpl(_, _))
