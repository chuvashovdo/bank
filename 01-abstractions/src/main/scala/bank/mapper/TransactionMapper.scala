package bank.mapper

import zio.{ Task, ZIO }
import bank.models.Transaction
import bank.entity.TransactionEntity

object TransactionMapper:
  def toModel(entity: TransactionEntity): Task[Transaction] =
    ZIO.succeed:
      Transaction(
        id = entity.id,
        sourceAccountId = entity.sourceAccountId,
        destinationAccountId = entity.destinationAccountId,
        amount = entity.amount,
        currency = entity.currency,
        memo = entity.memo,
        createdAt = entity.createdAt,
      )

  def toEntity(model: Transaction): TransactionEntity =
    TransactionEntity(
      id = model.id,
      sourceAccountId = model.sourceAccountId,
      destinationAccountId = model.destinationAccountId,
      amount = model.amount,
      currency = model.currency,
      memo = model.memo,
      createdAt = model.createdAt,
    )
