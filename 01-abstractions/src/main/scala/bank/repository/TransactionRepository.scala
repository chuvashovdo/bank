package bank.repository

import zio.*
import scala.math.BigDecimal
import java.time.Instant
import java.util.UUID
import bank.entity.TransactionEntity

trait TransactionRepository:
  def create(transaction: TransactionEntity): Task[TransactionEntity]

  def findById(id: UUID): Task[TransactionEntity]

  def findByAccountId(
    accountId: UUID,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): Task[List[TransactionEntity]]

object TransactionRepository:
  def create(transaction: TransactionEntity): RIO[TransactionRepository, TransactionEntity] =
    ZIO.serviceWithZIO[TransactionRepository](_.create(transaction))

  def findById(id: UUID): RIO[TransactionRepository, TransactionEntity] =
    ZIO.serviceWithZIO[TransactionRepository](_.findById(id))

  def findByAccountId(
    accountId: UUID,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): RIO[TransactionRepository, List[TransactionEntity]] =
    ZIO.serviceWithZIO[TransactionRepository](
      _.findByAccountId(accountId, limit, offset, minAmount, maxAmount, startDate, endDate)
    )
