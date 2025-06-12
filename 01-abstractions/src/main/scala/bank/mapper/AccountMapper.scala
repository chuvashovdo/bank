package bank.mapper

import zio.{ ZIO, Task }
import bank.models.{ Account, Balance }
import bank.entity.AccountEntity
import user.models.UserId

object AccountMapper:
  def toModel(entity: AccountEntity): Task[Account] =
    for balance <- ZIO.fromEither(Balance(entity.balance)).mapError(e => new Exception(e.details))
    yield Account(
      id = entity.id,
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
      id = model.id,
      userId = model.userId.value,
      accountNumber = model.accountNumber,
      balance = model.balance.value,
      currency = model.currency,
      accountStatus = model.accountStatus,
      createdAt = model.createdAt,
      updatedAt = model.updatedAt,
    )
