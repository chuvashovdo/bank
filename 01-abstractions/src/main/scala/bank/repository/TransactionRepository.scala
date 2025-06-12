package bank.repository

import zio.*
import java.util.UUID
import bank.models.Transaction
import scala.math.BigDecimal
import java.time.Instant

trait TransactionRepository:
  def create(transaction: Transaction): Task[Transaction]
  def findByAccountId(
    accountId: UUID,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): Task[List[Transaction]]

object TransactionRepository:
  def create(transaction: Transaction): RIO[TransactionRepository, Transaction] =
    ZIO.serviceWithZIO[TransactionRepository](_.create(transaction))

  def findByAccountId(
    accountId: UUID,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): RIO[TransactionRepository, List[Transaction]] =
    ZIO.serviceWithZIO[TransactionRepository](
      _.findByAccountId(accountId, limit, offset, minAmount, maxAmount, startDate, endDate)
    )
