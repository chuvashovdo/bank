package bank.api

import zio.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint

import common.api.ApiEndpoint
import common.models.ErrorResponse
import common.TapirSchemas.given
import user.models.UserId
import jwt.service.JwtService
import bank.service.AccountService
import bank.models.dto.{ AccountResponse, CreateAccountRequest }
import bank.models.{ Account, AccountId }

class AccountEndpoints(
  accountService: AccountService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val accountsPath =
    "api" / "accounts"

  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  private val adminEndpoint =
    createSecuredEndpointWithRoles(jwtService, "ADMIN")

  private val createAccountsEndpoint =
    createSecuredEndpointWithPermissions(jwtService, "accounts:create")

  val createAccountEndpoint: ServerEndpoint[Any, Task] =
    createAccountsEndpoint
      .post
      .in(accountsPath)
      .tag("Bank Accounts")
      .summary("Создать новый банковский счет")
      .in(jsonBody[CreateAccountRequest])
      .out(jsonBody[AccountResponse])
      .serverLogic { authContext => request =>
        val userId = authContext.userId
        handleCreateAccount(userId, request).either
      }

  val listAccountsEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(accountsPath)
      .tag("Bank Accounts")
      .summary("Получить список счетов пользователя")
      .out(jsonBody[List[AccountResponse]])
      .serverLogic { authContext => _ =>
        val userId = authContext.userId
        handleListAccounts(userId).either
      }

  val getAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(accountsPath / path[AccountId]("accountId"))
      .tag("Bank Accounts")
      .summary("Получить информацию о конкретном счете")
      .out(jsonBody[AccountResponse])
      .serverLogic { authContext => accountId =>
        ZIO
          .fromEither(
            checkPermissions(authContext, s"${accountsPath.toString()}/$accountId", "accounts:read")
          )
          .flatMap { _ =>
            val userId = authContext.userId
            handleGetAccount(userId, accountId)
          }
          .either
      }

  val closeAccountEndpoint: ServerEndpoint[Any, Task] =
    adminEndpoint
      .delete
      .in(accountsPath / path[AccountId]("accountId"))
      .tag("Bank Accounts")
      .summary("Закрыть банковский счет (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => accountId =>
        val userId = authContext.userId
        handleCloseAccount(userId, accountId).either
      }

  val adminAccountsEndpoint: ServerEndpoint[Any, Task] =
    adminEndpoint
      .get
      .in("api" / "admin" / "accounts")
      .tag("Admin Bank Accounts")
      .summary("Управление счетами (только для администратора)")
      .out(jsonBody[List[AccountResponse]])
      .serverLogic { authContext => _ =>
        val userId = authContext.userId

        handleListAccounts(userId).map { accounts =>
          accounts
        }.either
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      createAccountEndpoint,
      listAccountsEndpoint,
      getAccountEndpoint,
      closeAccountEndpoint,
      adminAccountsEndpoint,
    )

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
    accountId: AccountId,
  ): ZIO[Any, ErrorResponse, AccountResponse] =
    accountService
      .getAccount(accountId, userId)
      .map(accountToResponse)
      .mapError(handleCommonErrors(s"${accountsPath.toString()}/$accountId"))

  private def handleCloseAccount(
    userId: UserId,
    accountId: AccountId,
  ): ZIO[Any, ErrorResponse, Unit] =
    accountService
      .closeAccount(accountId, userId)
      .mapError(handleCommonErrors(s"${accountsPath.toString()}/$accountId"))

  private def accountToResponse(account: Account): AccountResponse =
    AccountResponse(
      id = account.id,
      accountNumber = account.accountNumber,
      userId = account.userId,
      balance = account.balance,
      currency = account.currency,
      status = account.accountStatus,
    )
