package bank.models.dto

import java.util.UUID
import zio.json.{ DeriveJsonCodec, JsonCodec }

import bank.models.{ AccountStatus, Balance }

// --- Requests ---

case class CreateAccountRequest(currency: String)

object CreateAccountRequest:
  given codec: JsonCodec[CreateAccountRequest] =
    DeriveJsonCodec.gen

// --- Responses ---

case class AccountResponse(
  id: UUID,
  accountNumber: String,
  userId: UUID,
  balance: Balance,
  currency: String,
  status: AccountStatus,
)

object AccountResponse:
  given codec: JsonCodec[AccountResponse] =
    DeriveJsonCodec.gen
