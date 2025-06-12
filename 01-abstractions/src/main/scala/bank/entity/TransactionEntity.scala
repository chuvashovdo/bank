package bank.entity

import java.util.UUID
import java.time.Instant
import scala.math.BigDecimal

final case class TransactionEntity(
  id: UUID,
  sourceAccountId: Option[UUID],
  destinationAccountId: Option[UUID],
  amount: BigDecimal,
  currency: String,
  memo: Option[String],
  createdAt: Instant,
)
