package bank.models

import zio.json.*
import java.time.Instant
import scala.math.BigDecimal

final case class Transaction(
  id: TransactionId,
  sourceAccountId: Option[AccountId],
  destinationAccountId: Option[AccountId],
  amount: BigDecimal,
  currency: String,
  memo: Option[String],
  createdAt: Instant,
)

object Transaction:
  given JsonCodec[Transaction] =
    DeriveJsonCodec.gen[Transaction]
