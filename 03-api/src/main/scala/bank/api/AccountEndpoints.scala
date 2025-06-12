package bank.api

import zio.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import java.util.UUID

import common.api.ApiEndpoint
import common.models.ErrorResponse
import common.TapirSchemas.given
import user.models.UserId
import jwt.service.JwtService
import bank.service.AccountService
import bank.models.dto.{ AccountResponse, CreateAccountRequest }
import bank.models.Account

class AccountEndpoints(
  accountService: AccountService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val accountsPath =
    "api" / "accounts"
  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  // --- Endpoints Definition ---

  val createAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .post
      .in(accountsPath)
      .tag("Bank Accounts")
      .summary("Создать новый банковский счет")
      .in(jsonBody[CreateAccountRequest])
      .out(jsonBody[AccountResponse])
      .serverLogic(userId => request => handleCreateAccount(userId, request).either)

  val listAccountsEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(accountsPath)
      .tag("Bank Accounts")
      .summary("Получить список счетов пользователя")
      .out(jsonBody[List[AccountResponse]])
      .serverLogic(userId => _ => handleListAccounts(userId).either)

  val getAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(accountsPath / path[UUID]("accountId"))
      .tag("Bank Accounts")
      .summary("Получить информацию о конкретном счете")
      .out(jsonBody[AccountResponse])
      .serverLogic(userId => accountId => handleGetAccount(userId, accountId).either)

  val closeAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .delete
      .in(accountsPath / path[UUID]("accountId"))
      .tag("Bank Accounts")
      .summary("Закрыть банковский счет")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic(userId => accountId => handleCloseAccount(userId, accountId).either)

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      createAccountEndpoint,
      listAccountsEndpoint,
      getAccountEndpoint,
      closeAccountEndpoint,
    )

  // --- Handlers ---

  private def handleCreateAccount(
    userId: UserId,
    request: CreateAccountRequest,
  ): ZIO[Any, ErrorResponse, AccountResponse] =
    accountService
      .createAccount(userId, request.currency)
      .map(accountToResponse)
      .mapError(handleCommonErrors(accountsPath.toString()))

  private def handleListAccounts(userId: UserId): ZIO[Any, ErrorResponse, List[AccountResponse]] =
    accountService
      .listAccountsForUser(userId)
      .map(_.map(accountToResponse))
      .mapError(handleCommonErrors(accountsPath.toString()))

  private def handleGetAccount(
    userId: UserId,
    accountId: UUID,
  ): ZIO[Any, ErrorResponse, AccountResponse] =
    accountService
      .getAccount(accountId, userId)
      .map(accountToResponse)
      .mapError(handleCommonErrors(s"${accountsPath.toString()}/$accountId"))

  private def handleCloseAccount(
    userId: UserId,
    accountId: UUID,
  ): ZIO[Any, ErrorResponse, Unit] =
    accountService
      .closeAccount(accountId, userId)
      .mapError(handleCommonErrors(s"${accountsPath.toString()}/$accountId"))

  // --- Mappers ---

  private def accountToResponse(account: Account): AccountResponse =
    AccountResponse(
      id = account.id,
      accountNumber = account.accountNumber,
      userId = account.userId.value,
      balance = account.balance,
      currency = account.currency,
      status = account.accountStatus,
    )
