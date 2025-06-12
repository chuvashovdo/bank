package bank.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import java.util.UUID
import scala.math.BigDecimal
import scala.annotation.nowarn
import java.sql.Types
import scala.CanEqual.derived

import bank.entity.AccountEntity
import bank.errors.*
import bank.models.AccountStatus

class AccountRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends AccountRepository:
  import quill.*

  given CanEqual[UUID, UUID] =
    derived

  @nowarn("msg=unused")
  inline private given accountSchemaMeta: SchemaMeta[AccountEntity] =
    schemaMeta(
      "accounts",
      _.id -> "id",
      _.userId -> "user_id",
      _.accountNumber -> "account_number",
      _.balance -> "balance",
      _.currency -> "currency",
      _.accountStatus -> "account_status",
      _.createdAt -> "created_at",
      _.updatedAt -> "updated_at",
    )

  implicit private val accountStatusEncoder: Encoder[AccountStatus] =
    encoder(
      Types.OTHER,
      (
        index,
        value,
        row,
      ) => row.setObject(index, value.toString, Types.OTHER),
    )
  implicit private val accountStatusDecoder: Decoder[AccountStatus] =
    decoder(
      (
        index,
        row,
        session,
      ) => AccountStatus.valueOf(row.getObject(index).toString)
    )

  override def create(account: AccountEntity): Task[AccountEntity] =
    run(quote {
      query[AccountEntity].insertValue(lift(account))
    }) *> ZIO.succeed(account)

  override def findById(id: UUID): Task[AccountEntity] =
    run(quote(query[AccountEntity].filter(_.id.equals(lift(id)))))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundError(id)))

  override def findByAccountNumber(accountNumber: String): Task[AccountEntity] =
    run(quote(query[AccountEntity].filter(_.accountNumber == lift(accountNumber))))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundByNumberError(accountNumber)))

  override def findByUserId(userId: UUID): Task[List[AccountEntity]] =
    run(quote(query[AccountEntity].filter(_.userId.equals(lift(userId)))))

  override def updateStatus(id: UUID, newStatus: AccountStatus): Task[Unit] =
    run(quote {
      query[AccountEntity]
        .filter(_.id.equals(lift(id)))
        .update(_.accountStatus -> lift(newStatus))
    }).unit

  override def updateBalance(id: UUID, newBalance: BigDecimal): Task[Unit] =
    run(quote {
      query[AccountEntity]
        .filter(_.id.equals(lift(id)))
        .update(_.balance -> lift(newBalance))
    }).unit

  override def delete(id: UUID): Task[Unit] =
    run(quote(query[AccountEntity].filter(_.id.equals(lift(id))).delete)).unit

  override def getNextAccountNumber: Task[Long] =
    run(quote {
      query[AccountEntity].map(a => a.accountNumber)
    }).map { accountNumbers =>
      val max = accountNumbers.map(_.toLong).maxOption.getOrElse(1000000000L)
      max + 1
    }

object AccountRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], AccountRepository] =
    ZLayer.fromFunction(new AccountRepositoryImpl(_))
