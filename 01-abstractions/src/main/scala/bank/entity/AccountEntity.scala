package bank.entity

import java.util.UUID
import java.time.Instant
import scala.math.BigDecimal
import bank.models.AccountStatus

final case class AccountEntity(
  id: UUID,
  userId: UUID,
  accountNumber: String,
  balance: BigDecimal,
  currency: String,
  accountStatus: AccountStatus,
  createdAt: Instant,
  updatedAt: Instant,
)
