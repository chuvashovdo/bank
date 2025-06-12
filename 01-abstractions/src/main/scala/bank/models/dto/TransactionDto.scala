package bank.models.dto

import scala.math.BigDecimal
import zio.json.{ DeriveJsonCodec, JsonCodec }
import bank.models.{ AccountId, TransactionId }

// --- Requests ---

case class TransactionRequest(
  amount: BigDecimal,
  memo: Option[String],
)

object TransactionRequest:
  given codec: JsonCodec[TransactionRequest] =
    DeriveJsonCodec.gen

case class TransferRequest(
  destinationAccountId: AccountId,
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
  id: TransactionId,
  sourceAccountId: Option[AccountId],
  destinationAccountId: Option[AccountId],
  amount: BigDecimal,
  currency: String,
  memo: Option[String],
  createdAt: java.time.Instant,
)

object TransactionResponse:
  given codec: JsonCodec[TransactionResponse] =
    DeriveJsonCodec.gen
