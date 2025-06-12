package bank.errors

import common.errors.BusinessError
import java.util.UUID
import scala.math.BigDecimal

sealed trait BankError extends BusinessError

final case class InsufficientFundsError(
  accountId: UUID,
  requestedAmount: BigDecimal,
  actualBalance: BigDecimal,
) extends BankError:
  override val errorCode: String =
    "INSUFFICIENT_FUNDS"
  override def message: String =
    s"Insufficient funds on account $accountId. Requested: $requestedAmount, but balance is: $actualBalance"

final case class AccountNotFoundError(accountId: UUID) extends BankError:
  override val errorCode: String =
    "ACCOUNT_NOT_FOUND"
  override def message: String =
    s"Account with id $accountId not found."

final case class AccountNotFoundByNumberError(accountNumber: String) extends BankError:
  override val errorCode: String =
    "ACCOUNT_NOT_FOUND"
  override def message: String =
    s"Account with number $accountNumber not found."

final case class AccountClosedError(accountId: UUID) extends BankError:
  override val errorCode: String =
    "ACCOUNT_CLOSED"
  override def message: String =
    s"Account $accountId is closed and cannot be used for operations."

final case class UnauthorizedAccountAccessError(userId: UUID, accountId: UUID) extends BankError:
  override val errorCode: String =
    "UNAUTHORIZED_ACCOUNT_ACCESS"
  override def message: String =
    s"User $userId does not have permission to access account $accountId."

final case class CurrencyMismatchError(
  sourceAccountCurrency: String,
  destinationAccountCurrency: String,
) extends BankError:
  override val errorCode: String =
    "CURRENCY_MISMATCH"
  override val message: String =
    s"Currency mismatch: source account currency $sourceAccountCurrency does not match destination account currency $destinationAccountCurrency"

final case class CannotCloseAccountWithNonZeroBalanceError(
  accountId: UUID,
  balance: BigDecimal,
) extends BankError:
  override val errorCode: String =
    "NON_ZERO_BALANCE"
  override val message: String =
    s"Cannot close account $accountId because it has a non-zero balance: $balance"

final case class CannotTransferToSameAccountError(accountId: UUID) extends BankError:
  override val errorCode: String =
    "SELF_TRANSFER_NOT_ALLOWED"
  override val message: String =
    s"Cannot transfer funds to the same account: $accountId"

final case class TransactionNotFoundError(transactionId: UUID) extends BankError:
  override val errorCode: String =
    "TRANSACTION_NOT_FOUND"
  override val message: String =
    s"Transaction with id $transactionId not found."
