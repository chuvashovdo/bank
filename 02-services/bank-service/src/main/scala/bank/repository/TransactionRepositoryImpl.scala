package bank.repository

import io.getquill.*
import io.getquill.extras.*
import io.getquill.jdbczio.Quill
import zio.*
import java.util.UUID
import scala.annotation.nowarn
import scala.math.BigDecimal
import java.time.Instant

import bank.entity.TransactionEntity
import bank.errors.TransactionNotFoundError

class TransactionRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends TransactionRepository:
  import quill.*

  @nowarn("msg=unused")
  inline private given transactionSchemaMeta: SchemaMeta[TransactionEntity] =
    schemaMeta(
      "app_transactions",
      _.id -> "id",
      _.sourceAccountId -> "source_account_id",
      _.destinationAccountId -> "destination_account_id",
      _.amount -> "amount",
      _.currency -> "currency",
      _.memo -> "memo",
      _.createdAt -> "created_at",
    )

  override def create(transaction: TransactionEntity): Task[TransactionEntity] =
    run(quote {
      query[TransactionEntity].insertValue(lift(transaction))
    }) *> ZIO.succeed(transaction)

  override def findById(id: UUID): Task[TransactionEntity] =
    run(quote(query[TransactionEntity].filter(_.id.equals(lift(id)))))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => TransactionNotFoundError(id)))

  override def findByAccountId(
    accountId: UUID,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): Task[List[TransactionEntity]] =
    type QueryTransformer =
      Quoted[EntityQuery[TransactionEntity]] => Quoted[EntityQuery[TransactionEntity]]

    val baseQuery =
      quote:
        query[TransactionEntity]
          .filter(t =>
            t.sourceAccountId.contains(lift(accountId)) ||
            t.destinationAccountId.contains(lift(accountId))
          )

    val withMinAmount: Option[QueryTransformer] =
      minAmount.map(min => q => quote(q.filter(t => t.amount >= lift(min))))

    val withMaxAmount: Option[QueryTransformer] =
      maxAmount.map(max => q => quote(q.filter(t => t.amount <= lift(max))))

    val withStartDate: Option[QueryTransformer] =
      startDate.map(start => q => quote(q.filter(t => t.createdAt >= lift(start))))

    val withEndDate: Option[QueryTransformer] =
      endDate.map(end => q => quote(q.filter(t => t.createdAt <= lift(end))))

    val filters = List(withMinAmount, withMaxAmount, withStartDate, withEndDate)
    val finalQuery = filters.flatten.foldLeft(baseQuery)((q, f) => f(q))

    run(quote {
      finalQuery
        .sortBy(_.createdAt)(Ord.desc)
        .drop(lift(offset))
        .take(lift(limit))
    })

object TransactionRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
