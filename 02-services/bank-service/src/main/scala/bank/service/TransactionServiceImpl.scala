package bank.service

import zio.*
import scala.math.BigDecimal
import common.service.Transactor
import java.time.Instant
import java.util.UUID

import bank.models.{ Account, AccountId, AccountStatus, Transaction, TransactionId }
import bank.repository.{ AccountRepository, TransactionRepository }
import user.models.UserId
import bank.errors.*
import bank.entity.TransactionEntity
import bank.mapper.{ AccountMapper, TransactionMapper }

class TransactionServiceImpl(
  accountRepository: AccountRepository,
  transactionRepository: TransactionRepository,
  transactor: Transactor,
) extends TransactionService:
  private def getAndAuthorizeAccount(accountId: AccountId, userId: UserId): Task[Account] =
    for
      accountEntity <- accountRepository.findById(accountId.value)
      account <- AccountMapper.toModelFromEntity(accountEntity)
      _ <-
        ZIO.when(!account.userId.equals(userId))(
          ZIO.fail(UnauthorizedAccountAccessError(userId.value, accountId.value))
        )
    yield account

  override def performTransfer(
    sourceAccountId: AccountId,
    destinationAccountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        _ <-
          ZIO.when(sourceAccountId.equals(destinationAccountId))(
            ZIO.fail(CannotTransferToSameAccountError(sourceAccountId.value))
          )
        sourceAccount <- getAndAuthorizeAccount(sourceAccountId, userId)
        destinationAccountEntity <- accountRepository.findById(destinationAccountId.value)
        destinationAccount <- AccountMapper.toModelFromEntity(destinationAccountEntity)
        _ <-
          ZIO.unless(sourceAccount.accountStatus == AccountStatus.OPEN)(
            ZIO.fail(AccountClosedError(sourceAccountId.value))
          )
        _ <-
          ZIO.unless(destinationAccount.accountStatus == AccountStatus.OPEN)(
            ZIO.fail(AccountClosedError(destinationAccountId.value))
          )
        _ <-
          ZIO.when(sourceAccount.currency != destinationAccount.currency)(
            ZIO.fail(CurrencyMismatchError(sourceAccount.currency, destinationAccount.currency))
          )
        _ <-
          ZIO.when(sourceAccount.balance.value < amount)(
            ZIO.fail(
              InsufficientFundsError(sourceAccountId.value, amount, sourceAccount.balance.value)
            )
          )
        now <- ZIO.succeed(Instant.now())
        newSourceBalance = sourceAccount.balance.value - amount
        newDestinationBalance = destinationAccount.balance.value + amount
        _ <- accountRepository.updateBalance(sourceAccountId.value, newSourceBalance)
        _ <- accountRepository.updateBalance(destinationAccountId.value, newDestinationBalance)
        transactionEntity =
          TransactionEntity(
            id = UUID.randomUUID(),
            sourceAccountId = Some(sourceAccountId.value),
            destinationAccountId = Some(destinationAccountId.value),
            amount = amount,
            currency = sourceAccount.currency,
            memo = memo,
            createdAt = now,
          )
        createdEntity <- transactionRepository.create(transactionEntity)
        createdTransaction <- TransactionMapper.toModelFromEntity(createdEntity)
      yield createdTransaction

  override def performTransferByAccountNumber(
    sourceAccountId: AccountId,
    destinationAccountNumber: String,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    for
      destinationAccountEntity <- accountRepository.findByAccountNumber(destinationAccountNumber)
      destinationAccount <- AccountMapper.toModelFromEntity(destinationAccountEntity)
      transaction <- performTransfer(sourceAccountId, destinationAccount.id, amount, memo, userId)
    yield transaction

  override def deposit(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        account <- getAndAuthorizeAccount(accountId, userId)
        _ <-
          ZIO.unless(account.accountStatus == AccountStatus.OPEN)(
            ZIO.fail(AccountClosedError(accountId.value))
          )
        now <- ZIO.succeed(Instant.now())
        newBalance = account.balance.value + amount
        _ <- accountRepository.updateBalance(accountId.value, newBalance)
        transactionEntity =
          TransactionEntity(
            id = UUID.randomUUID(),
            sourceAccountId = None,
            destinationAccountId = Some(accountId.value),
            amount = amount,
            currency = account.currency,
            memo = memo,
            createdAt = now,
          )
        createdEntity <- transactionRepository.create(transactionEntity)
        createdTransaction <- TransactionMapper.toModelFromEntity(createdEntity)
      yield createdTransaction

  override def withdraw(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction] =
    transactor.transact:
      for
        account <- getAndAuthorizeAccount(accountId, userId)
        _ <-
          ZIO.unless(account.accountStatus == AccountStatus.OPEN)(
            ZIO.fail(AccountClosedError(accountId.value))
          )
        _ <-
          ZIO.when(account.balance.value < amount)(
            ZIO.fail(InsufficientFundsError(accountId.value, amount, account.balance.value))
          )
        now <- ZIO.succeed(Instant.now())
        newBalance = account.balance.value - amount
        _ <- accountRepository.updateBalance(accountId.value, newBalance)
        transactionEntity =
          TransactionEntity(
            id = UUID.randomUUID(),
            sourceAccountId = Some(accountId.value),
            destinationAccountId = None,
            amount = amount,
            currency = account.currency,
            memo = memo,
            createdAt = now,
          )
        createdEntity <- transactionRepository.create(transactionEntity)
        createdTransaction <- TransactionMapper.toModelFromEntity(createdEntity)
      yield createdTransaction

  override def getAccountTransactions(
    accountId: AccountId,
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
      transactionEntities <-
        transactionRepository.findByAccountId(
          accountId.value,
          limit,
          offset,
          minAmount,
          maxAmount,
          startDate,
          endDate,
        )
      transactions <- ZIO.foreach(transactionEntities)(TransactionMapper.toModelFromEntity)
    yield transactions

  override def getTransactionById(id: TransactionId, userId: UserId): Task[Transaction] =
    for
      txEntity <- transactionRepository.findById(id.value)
      sourceAccountOpt <- ZIO.foreach(txEntity.sourceAccountId)(accountRepository.findById)
      destAccountOpt <- ZIO.foreach(txEntity.destinationAccountId)(accountRepository.findById)
      userHasAccess =
        sourceAccountOpt.exists(_.userId.equals(userId.value)) ||
        destAccountOpt.exists(_.userId.equals(userId.value))
      _ <- ZIO.unless(userHasAccess)(ZIO.fail(TransactionNotFoundError(id.value)))
      transaction <- TransactionMapper.toModelFromEntity(txEntity)
    yield transaction

object TransactionServiceImpl:
  val layer: ZLayer[AccountRepository & TransactionRepository & Transactor, Nothing, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceImpl(_, _, _))
