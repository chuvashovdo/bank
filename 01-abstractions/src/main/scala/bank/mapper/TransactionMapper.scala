package bank.mapper

import zio.{ Task, ZIO }
import bank.models.Transaction
import bank.entity.TransactionEntity
import bank.models.TransactionId
import bank.models.AccountId

object TransactionMapper:
  def toModelFromEntity(entity: TransactionEntity): Task[Transaction] =
    ZIO.succeed:
      Transaction(
        id = TransactionId(entity.id),
        sourceAccountId = entity.sourceAccountId.map(AccountId(_)),
        destinationAccountId = entity.destinationAccountId.map(AccountId(_)),
        amount = entity.amount,
        currency = entity.currency,
        memo = entity.memo,
        createdAt = entity.createdAt,
      )

  def toEntityFromModel(model: Transaction): TransactionEntity =
    TransactionEntity(
      id = model.id.value,
      sourceAccountId = model.sourceAccountId.map(_.value),
      destinationAccountId = model.destinationAccountId.map(_.value),
      amount = model.amount,
      currency = model.currency,
      memo = model.memo,
      createdAt = model.createdAt,
    )
