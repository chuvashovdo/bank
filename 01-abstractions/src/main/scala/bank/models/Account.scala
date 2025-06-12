package bank.models

import zio.json.*
import java.time.Instant
import user.models.UserId

final case class Account(
  id: AccountId,
  userId: UserId,
  accountNumber: String,
  balance: Balance,
  currency: String,
  accountStatus: AccountStatus,
  createdAt: Instant,
  updatedAt: Instant,
)

object Account:
  given JsonCodec[Account] =
    DeriveJsonCodec.gen[Account]
