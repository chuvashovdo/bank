package bank.mapper

import zio.{ ZIO, Task }
import bank.models.{ Account, Balance }
import bank.entity.AccountEntity
import user.models.UserId
import bank.models.AccountId

object AccountMapper:
  def toModel(entity: AccountEntity): Task[Account] =
    for balance <- ZIO.fromEither(Balance(entity.balance))
    yield Account(
      id = AccountId(entity.id),
      userId = UserId(entity.userId),
      accountNumber = entity.accountNumber,
      balance = balance,
      currency = entity.currency,
      accountStatus = entity.accountStatus,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
    )

  def toEntity(model: Account): AccountEntity =
    AccountEntity(
      id = model.id.value,
      userId = model.userId.value,
      accountNumber = model.accountNumber,
      balance = model.balance.value,
      currency = model.currency,
      accountStatus = model.accountStatus,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
