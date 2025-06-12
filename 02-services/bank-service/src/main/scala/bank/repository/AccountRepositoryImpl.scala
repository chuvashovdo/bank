package bank.repository

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import java.util.UUID
import scala.math.BigDecimal
import scala.annotation.nowarn
import java.sql.Types

import bank.entity.AccountEntity
import bank.errors.*
import bank.mapper.AccountMapper
import bank.models.{ Account, AccountStatus }

class AccountRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends AccountRepository:
  import quill.*

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

  override def create(account: Account): Task[Account] =
    val entity = AccountMapper.toEntity(account)
    run(quote {
      query[AccountEntity].insertValue(lift(entity))
    }) *> ZIO.succeed(account)

  override def findById(id: UUID): Task[Account] =
    run(quote(query[AccountEntity].filter(_.id.equals(lift(id)))))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundError(id)))
      .flatMap(AccountMapper.toModel)

  override def findByAccountNumber(accountNumber: String): Task[Account] =
    run(quote(query[AccountEntity].filter(_.accountNumber.equals(lift(accountNumber)))))
      .map(_.headOption)
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundErrorByNumber(accountNumber)))
      .flatMap(AccountMapper.toModel)

  override def findByUserId(userId: UUID): Task[List[Account]] =
    run(quote(query[AccountEntity].filter(_.userId.equals(lift(userId)))))
      .flatMap(ZIO.foreach(_)(AccountMapper.toModel))

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

object AccountRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], AccountRepository] =
    ZLayer.fromFunction(new AccountRepositoryImpl(_))
