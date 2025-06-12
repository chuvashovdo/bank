package bank.models

import zio.json.*
import java.util.UUID
import java.time.Instant
import scala.math.BigDecimal

final case class Transaction(
  id: UUID,
  sourceAccountId: Option[UUID],
  destinationAccountId: Option[UUID],
  amount: BigDecimal,
  currency: String,
  memo: Option[String],
  createdAt: Instant,
)

object Transaction:
  given JsonCodec[Transaction] =
    DeriveJsonCodec.gen[Transaction]
