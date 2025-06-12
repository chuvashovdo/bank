package bank.models.dto

import java.util.UUID
import scala.math.BigDecimal
import zio.json.{ DeriveJsonCodec, JsonCodec }

// --- Requests ---

case class TransactionRequest(
  amount: BigDecimal,
  memo: Option[String],
)

object TransactionRequest:
  given codec: JsonCodec[TransactionRequest] =
    DeriveJsonCodec.gen

case class TransferRequest(
  destinationAccountId: UUID,
  amount: BigDecimal,
  memo: Option[String],
)

object TransferRequest:
  given codec: JsonCodec[TransferRequest] =
    DeriveJsonCodec.gen

case class TransferByAccountRequest(
  destinationAccountNumber: String,
  amount: BigDecimal,
  memo: Option[String],
)

object TransferByAccountRequest:
  given codec: JsonCodec[TransferByAccountRequest] =
    DeriveJsonCodec.gen

// --- Responses ---

case class TransactionResponse(
  id: UUID,
  sourceAccountId: Option[UUID],
  destinationAccountId: Option[UUID],
  amount: BigDecimal,
  currency: String,
  memo: Option[String],
  createdAt: java.time.Instant,
)

object TransactionResponse:
  given codec: JsonCodec[TransactionResponse] =
    DeriveJsonCodec.gen
