package bank.models.dto

import zio.json.{ DeriveJsonCodec, JsonCodec }
import user.models.UserId
import bank.models.{ AccountId, AccountStatus, Balance }

// --- Requests ---

case class CreateAccountRequest(currency: String)

object CreateAccountRequest:
  given codec: JsonCodec[CreateAccountRequest] =
    DeriveJsonCodec.gen

case class UpdateAccountStatusRequest(status: AccountStatus)

object UpdateAccountStatusRequest:
  given codec: JsonCodec[UpdateAccountStatusRequest] =
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
