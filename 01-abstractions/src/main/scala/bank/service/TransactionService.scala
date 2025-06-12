package bank.service

import zio.*
import scala.math.BigDecimal
import bank.models.Transaction
import user.models.UserId
import java.time.Instant
import bank.models.AccountId
import bank.models.TransactionId

trait TransactionService:
  def performTransfer(
    sourceAccountId: AccountId,
    destinationAccountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction]

  def performTransferByAccountNumber(
    sourceAccountId: AccountId,
    destinationAccountNumber: String,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction]

  def deposit(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction]

  def withdraw(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): Task[Transaction]

  def getAccountTransactions(
    accountId: AccountId,
    userId: UserId,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): Task[List[Transaction]]

  def getTransactionById(id: TransactionId, userId: UserId): Task[Transaction]

object TransactionService:
  def performTransfer(
    sourceAccountId: AccountId,
    destinationAccountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): RIO[TransactionService, Transaction] =
    ZIO.serviceWithZIO[TransactionService](
      _.performTransfer(sourceAccountId, destinationAccountId, amount, memo, userId)
    )

  def performTransferByAccountNumber(
    sourceAccountId: AccountId,
    destinationAccountNumber: String,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): RIO[TransactionService, Transaction] =
    ZIO.serviceWithZIO[TransactionService](
      _.performTransferByAccountNumber(
        sourceAccountId,
        destinationAccountNumber,
        amount,
        memo,
        userId,
      )
    )

  def deposit(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): RIO[TransactionService, Transaction] =
    ZIO.serviceWithZIO[TransactionService](_.deposit(accountId, amount, memo, userId))

  def withdraw(
    accountId: AccountId,
    amount: BigDecimal,
    memo: Option[String],
    userId: UserId,
  ): RIO[TransactionService, Transaction] =
    ZIO.serviceWithZIO[TransactionService](_.withdraw(accountId, amount, memo, userId))

  def getAccountTransactions(
    accountId: AccountId,
    userId: UserId,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): RIO[TransactionService, List[Transaction]] =
    ZIO.serviceWithZIO[TransactionService](
      _.getAccountTransactions(
        accountId,
        userId,
        limit,
        offset,
        minAmount,
        maxAmount,
        startDate,
        endDate,
      )
    )

  def getTransactionById(id: TransactionId, userId: UserId): RIO[TransactionService, Transaction] =
    ZIO.serviceWithZIO[TransactionService](_.getTransactionById(id, userId))
