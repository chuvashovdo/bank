package bank.service

import zio.*
import java.util.UUID
import scala.math.BigDecimal
import common.service.Transactor
import java.time.Instant

import bank.models.{ Account, AccountStatus, Transaction }
import bank.repository.{ AccountRepository, TransactionRepository }
import user.models.UserId
import bank.errors.*

class TransactionServiceImpl(
  accountRepository: AccountRepository,
  transactionRepository: TransactionRepository,
  transactor: Transactor,
) extends TransactionService:
  private def getAndAuthorizeAccount(accountId: UUID, userId: UserId): Task[Account] =
    for
      account <- accountRepository.findById(accountId)
      _ <-
        ZIO.when(account.userId != userId)(
          ZIO.fail(UnauthorizedAccountAccessError(userId.value, accountId))
        )
    yield account

  override def performTransfer(
    sourceAccountId: UUID,
    destinationAccountId: UUID,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        _ <-
          ZIO.when(sourceAccountId.equals(destinationAccountId))(
            ZIO.fail(CannotTransferToSameAccountError(sourceAccountId))
          )
        sourceAccount <- getAndAuthorizeAccount(sourceAccountId, userId)
        destinationAccount <- accountRepository.findById(destinationAccountId)
        _ <-
          ZIO.unless(sourceAccount.accountStatus.equals(AccountStatus.OPEN))(
            ZIO.fail(AccountClosedError(sourceAccountId))
          )
        _ <-
          ZIO.unless(destinationAccount.accountStatus.equals(AccountStatus.OPEN))(
            ZIO.fail(AccountClosedError(destinationAccountId))
          )
        _ <-
          ZIO.when(sourceAccount.currency != destinationAccount.currency)(
            ZIO.fail(CurrencyMismatchError(sourceAccount.currency, destinationAccount.currency))
          )
        _ <-
          ZIO.when(sourceAccount.balance.value < amount)(
            ZIO.fail(InsufficientFundsError(sourceAccountId, amount, sourceAccount.balance.value))
          )
        now <- Clock.instant
        newSourceBalance = sourceAccount.balance.value - amount
        newDestinationBalance = destinationAccount.balance.value + amount
        _ <- accountRepository.updateBalance(sourceAccountId, newSourceBalance)
        _ <- accountRepository.updateBalance(destinationAccountId, newDestinationBalance)
        transaction =
          Transaction(
            id = UUID.randomUUID(),
            sourceAccountId = Some(sourceAccountId),
            destinationAccountId = Some(destinationAccountId),
            amount = amount,
            currency = sourceAccount.currency,
            memo = memo,
            createdAt = now,
          )
        createdTransaction <- transactionRepository.create(transaction)
      yield createdTransaction

  override def performTransferByAccountNumber(
    sourceAccountId: UUID,
    destinationAccountNumber: String,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    for
      destinationAccount <- accountRepository.findByAccountNumber(destinationAccountNumber)
      transaction <- performTransfer(sourceAccountId, destinationAccount.id, amount, memo, userId)
    yield transaction

  override def deposit(
    accountId: UUID,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        account <- getAndAuthorizeAccount(accountId, userId)
        _ <-
          ZIO.unless(account.accountStatus.equals(AccountStatus.OPEN))(
            ZIO.fail(AccountClosedError(accountId))
          )
        now <- Clock.instant
        newBalance = account.balance.value + amount
        _ <- accountRepository.updateBalance(accountId, newBalance)
        transaction =
          Transaction(
            id = UUID.randomUUID(),
            sourceAccountId = None,
            destinationAccountId = Some(accountId),
            amount = amount,
            currency = account.currency,
            memo = memo,
            createdAt = now,
          )
        createdTransaction <- transactionRepository.create(transaction)
      yield createdTransaction

  override def withdraw(
    accountId: UUID,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        account <- getAndAuthorizeAccount(accountId, userId)
        _ <-
          ZIO.unless(account.accountStatus.equals(AccountStatus.OPEN))(
            ZIO.fail(AccountClosedError(accountId))
          )
        _ <-
          ZIO.when(account.balance.value < amount)(
            ZIO.fail(InsufficientFundsError(accountId, amount, account.balance.value))
          )
        now <- Clock.instant
        newBalance = account.balance.value - amount
        _ <- accountRepository.updateBalance(accountId, newBalance)
        transaction =
          Transaction(
            id = UUID.randomUUID(),
            sourceAccountId = Some(accountId),
            destinationAccountId = None,
            amount = amount,
            currency = account.currency,
            memo = memo,
            createdAt = now,
          )
        createdTransaction <- transactionRepository.create(transaction)
      yield createdTransaction

  override def getAccountTransactions(
    accountId: UUID,
    userId: UserId,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): Task[List[Transaction]] =
    for
      _ <- getAndAuthorizeAccount(accountId, userId)
      transactions <-
        transactionRepository.findByAccountId(
          accountId,
          limit,
          offset,
          minAmount,
          maxAmount,
          startDate,
          endDate,
        )
    yield transactions

object TransactionServiceImpl:
  val layer: ZLayer[AccountRepository & TransactionRepository & Transactor, Nothing, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceImpl(_, _, _))
