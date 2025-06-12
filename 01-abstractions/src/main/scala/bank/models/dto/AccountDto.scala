package bank.models.dto

import zio.json.{ DeriveJsonCodec, JsonCodec }
import user.models.UserId
import bank.models.{ AccountId, AccountStatus, Balance }

// --- Requests ---

case class CreateAccountRequest(currency: String)

object CreateAccountRequest:
  given codec: JsonCodec[CreateAccountRequest] =
    DeriveJsonCodec.gen

// --- Responses ---

case class AccountResponse(
  id: AccountId,
  accountNumber: String,
  userId: UserId,
  balance: Balance,
  currency: String,
  status: AccountStatus,
)

object AccountResponse:
  given codec: JsonCodec[AccountResponse] =
    DeriveJsonCodec.gen
