package bank.api

import zio.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint

import common.api.ApiEndpoint
import common.models.ErrorResponse
import common.TapirSchemas.given
import jwt.service.JwtService
import bank.service.AccountService
import bank.models.{ AccountId, AccountStatus }
import bank.models.dto.{ AccountResponse, UpdateAccountStatusRequest }
import bank.mapper.AccountMapper

class AccountAdminEndpoints(
  accountService: AccountService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val adminAccountsBasePath =
    "api" / "admin" / "accounts"

  private val adminSecuredEndpoint =
    createSecuredEndpointWithRoles(jwtService, "ADMIN")

  val listAccountsEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminAccountsBasePath)
      .tag("Admin - Account Management")
      .summary("Получить список всех счетов (только для администратора)")
      .out(jsonBody[List[AccountResponse]])
      .serverLogic { authContext => _ =>
        handleListAccounts().either
      }

  val getAccountByIdEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminAccountsBasePath / path[AccountId]("accountId"))
      .tag("Admin - Account Management")
      .summary("Получить информацию о счете по ID (только для администратора)")
      .out(jsonBody[AccountResponse])
      .serverLogic { authContext => accountId =>
        handleGetAccountById(accountId).either
      }

  val getAccountByNumberEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminAccountsBasePath / "number" / path[String]("accountNumber"))
      .tag("Admin - Account Management")
      .summary("Получить информацию о счете по номеру (только для администратора)")
      .out(jsonBody[AccountResponse])
      .serverLogic { authContext => accountNumber =>
        handleGetAccountByNumber(accountNumber).either
      }

  val getAccountsByUserIdEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .get
      .in(adminAccountsBasePath / "user" / path[java.util.UUID]("userId"))
      .tag("Admin - Account Management")
      .summary("Получить все счета пользователя (только для администратора)")
      .out(jsonBody[List[AccountResponse]])
      .serverLogic { authContext => userId =>
        handleGetAccountsByUserId(userId).either
      }

  val updateAccountStatusEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .put
      .in(adminAccountsBasePath / path[AccountId]("accountId") / "status")
      .tag("Admin - Account Management")
      .summary("Обновить статус счета (только для администратора)")
      .in(jsonBody[UpdateAccountStatusRequest])
      .out(jsonBody[AccountResponse])
      .serverLogic { authContext =>
        {
          case (accountId, updateRequest) =>
            handleUpdateAccountStatus(accountId, updateRequest.status).either
        }
      }

  val deleteAccountEndpoint: ServerEndpoint[Any, Task] =
    adminSecuredEndpoint
      .delete
      .in(adminAccountsBasePath / path[AccountId]("accountId"))
      .tag("Admin - Account Management")
      .summary("Удалить счет (только для администратора)")
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authContext => accountId =>
        handleDeleteAccount(accountId)
          .map(_ => Right(()))
          .catchAll(err => ZIO.succeed(Left(err)))
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      listAccountsEndpoint,
      getAccountByIdEndpoint,
      getAccountByNumberEndpoint,
      getAccountsByUserIdEndpoint,
      updateAccountStatusEndpoint,
      deleteAccountEndpoint,
    )

  private def handleListAccounts(): ZIO[Any, ErrorResponse, List[AccountResponse]] =
    accountService
      .findAllAccounts()
      .map(_.map(AccountMapper.toResponseFromModel(_)))
      .mapError(handleCommonErrors(adminAccountsBasePath.toString()))

  private def handleGetAccountById(accountId: AccountId): ZIO[Any, ErrorResponse, AccountResponse] =
    accountService
      .getAccountById(accountId)
      .map(AccountMapper.toResponseFromModel(_))
      .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/$accountId"))

  private def handleGetAccountByNumber(
    accountNumber: String
  ): ZIO[Any, ErrorResponse, AccountResponse] =
    accountService
      .getAccountByNumber(accountNumber)
      .map(AccountMapper.toResponseFromModel(_))
      .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/number/$accountNumber"))

  private def handleGetAccountsByUserId(
    userId: java.util.UUID
  ): ZIO[Any, ErrorResponse, List[AccountResponse]] =
    accountService
      .getAccountsByUserId(user.models.UserId(userId))
      .map(_.map(AccountMapper.toResponseFromModel(_)))
      .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/user/$userId"))

  private def handleUpdateAccountStatus(
    accountId: AccountId,
    newStatus: AccountStatus,
  ): ZIO[Any, ErrorResponse, AccountResponse] =
    for
      _ <-
        accountService
          .updateAccountStatus(accountId, newStatus)
          .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/$accountId"))

      updatedAccount <-
        accountService
          .getAccountById(accountId)
          .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/$accountId"))
    yield AccountMapper.toResponseFromModel(updatedAccount)

  private def handleDeleteAccount(accountId: AccountId): ZIO[Any, ErrorResponse, Unit] =
    accountService
      .deleteAccount(accountId)
      .mapError(handleCommonErrors(s"${adminAccountsBasePath.toString()}/$accountId"))
