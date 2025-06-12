package bank.api

import zio.*
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import java.time.Instant
import scala.math.BigDecimal

import common.api.ApiEndpoint
import common.models.ErrorResponse
import common.TapirSchemas.given
import user.models.UserId
import jwt.service.JwtService
import bank.service.TransactionService
import bank.models.dto.{
  // TransactionRequest,
  TransactionResponse,
  TransferRequest,
  TransferByAccountRequest,
}
import bank.models.{ AccountId, Transaction, TransactionId }

class TransactionEndpoints(
  transactionService: TransactionService,
  jwtService: JwtService,
) extends ApiEndpoint:
  private val transactionsBasePath =
    "api" / "transactions"
  private val accountsBasePath =
    "api" / "accounts" / path[AccountId]("accountId")
  private val securedEndpoint =
    createSecuredEndpoint(jwtService)

  // --- Endpoints Definition ---

  val getTransactionsEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(accountsBasePath / "transactions")
      .in(query[Int]("limit").default(20))
      .in(query[Int]("offset").default(0))
      .in(query[Option[BigDecimal]]("minAmount"))
      .in(query[Option[BigDecimal]]("maxAmount"))
      .in(query[Option[Instant]]("startDate"))
      .in(query[Option[Instant]]("endDate"))
      .tag("Bank Transactions")
      .summary("Получить историю транзакций по счету")
      .out(jsonBody[List[TransactionResponse]])
      .serverLogic { userId =>
        {
          case (
                 accountId: AccountId,
                 limit: Int,
                 offset: Int,
                 minAmount: Option[BigDecimal],
                 maxAmount: Option[BigDecimal],
                 startDate: Option[Instant],
                 endDate: Option[Instant],
               ) =>
            handleGetTransactions(
              userId,
              accountId,
              limit,
              offset,
              minAmount,
              maxAmount,
              startDate,
              endDate,
            ).either
        }
      }

  val getTransactionByIdEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .get
      .in(transactionsBasePath / path[TransactionId]("transactionId"))
      .tag("Bank Transactions")
      .summary("Получить информацию о конкретной транзакции")
      .out(jsonBody[TransactionResponse])
      .serverLogic { userId => transactionId =>
        handleGetTransactionById(userId, transactionId).either
      }

  /*
  val depositEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .post
      .in(accountsBasePath / "deposit")
      .tag("Bank Transactions")
      .summary("Пополнить счет")
      .in(jsonBody[TransactionRequest])
      .out(jsonBody[TransactionResponse])
      .serverLogic { userId =>
        {
          case (accountId: AccountId, request: TransactionRequest) =>
            handleDeposit(userId, accountId, request).either
        }
      }

  val withdrawEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .post
      .in(accountsBasePath / "withdraw")
      .tag("Bank Transactions")
      .summary("Снять средства со счета")
      .in(jsonBody[TransactionRequest])
      .out(jsonBody[TransactionResponse])
      .serverLogic { userId =>
        {
          case (accountId: AccountId, request: TransactionRequest) =>
            handleWithdraw(userId, accountId, request).either
        }
      }
   */

  val transferEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .post
      .in(accountsBasePath / "transfer")
      .tag("Bank Transactions")
      .summary("Перевести средства на другой счет")
      .in(jsonBody[TransferRequest])
      .out(jsonBody[TransactionResponse])
      .serverLogic { userId =>
        {
          case (sourceAccountId: AccountId, request: TransferRequest) =>
            handleTransfer(userId, sourceAccountId, request).either
        }
      }

  val transferByAccountEndpoint: ServerEndpoint[Any, Task] =
    securedEndpoint
      .post
      .in(accountsBasePath / "transfer-by-account")
      .tag("Bank Transactions")
      .summary("Перевести средства на другой счет по номеру счета")
      .in(jsonBody[TransferByAccountRequest])
      .out(jsonBody[TransactionResponse])
      .serverLogic { userId =>
        {
          case (sourceAccountId: AccountId, request: TransferByAccountRequest) =>
            handleTransferByAccount(userId, sourceAccountId, request).either
        }
      }

  val all: List[ServerEndpoint[Any, Task]] =
    List(
      getTransactionsEndpoint,
      getTransactionByIdEndpoint,
      // depositEndpoint,
      // withdrawEndpoint,
      transferEndpoint,
      transferByAccountEndpoint,
    )

  // --- Handlers ---

  private def handleGetTransactions(
    userId: UserId,
    accountId: AccountId,
    limit: Int,
    offset: Int,
    minAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    startDate: Option[Instant],
    endDate: Option[Instant],
  ): ZIO[Any, ErrorResponse, List[TransactionResponse]] =
    transactionService
      .getAccountTransactions(
        accountId,
        userId,
        limit,
        offset,
        minAmount,
        maxAmount,
        startDate,
        endDate,
      )
      .map(_.map(transactionToResponse))
      .mapError(handleCommonErrors(s"api/accounts/$accountId/transactions"))

  private def handleGetTransactionById(
    userId: UserId,
    transactionId: TransactionId,
  ): ZIO[Any, ErrorResponse, TransactionResponse] =
    transactionService
      .getTransactionById(transactionId, userId)
      .map(transactionToResponse)
      .mapError(handleCommonErrors(s"api/transactions/$transactionId"))

  /*
  private def handleDeposit(
    userId: UserId,
    accountId: AccountId,
    request: TransactionRequest,
  ): ZIO[Any, ErrorResponse, TransactionResponse] =
    transactionService
      .deposit(accountId, request.amount, request.memo, userId)
      .map(transactionToResponse)
      .mapError(handleCommonErrors(s"api/accounts/$accountId/deposit"))

  private def handleWithdraw(
    userId: UserId,
    accountId: AccountId,
    request: TransactionRequest,
  ): ZIO[Any, ErrorResponse, TransactionResponse] =
    transactionService
      .withdraw(accountId, request.amount, request.memo, userId)
      .map(transactionToResponse)
      .mapError(handleCommonErrors(s"api/accounts/$accountId/withdraw"))
   */

  private def handleTransfer(
    userId: UserId,
    sourceAccountId: AccountId,
    request: TransferRequest,
  ): ZIO[Any, ErrorResponse, TransactionResponse] =
    transactionService
      .performTransfer(
        sourceAccountId,
        request.destinationAccountId,
        request.amount,
        request.memo,
        userId,
      )
      .map(transactionToResponse)
      .mapError(handleCommonErrors(s"api/accounts/$sourceAccountId/transfer"))

  private def handleTransferByAccount(
    userId: UserId,
    sourceAccountId: AccountId,
    request: TransferByAccountRequest,
  ): ZIO[Any, ErrorResponse, TransactionResponse] =
    transactionService
      .performTransferByAccountNumber(
        sourceAccountId,
        request.destinationAccountNumber,
        request.amount,
        request.memo,
        userId,
      )
      .map(transactionToResponse)
      .mapError(handleCommonErrors(s"api/accounts/$sourceAccountId/transfer-by-account"))

  // --- Mappers ---

  private def transactionToResponse(transaction: Transaction): TransactionResponse =
    TransactionResponse(
      id = transaction.id,
      sourceAccountId = transaction.sourceAccountId,
      destinationAccountId = transaction.destinationAccountId,
      amount = transaction.amount,
      currency = transaction.currency,
      memo = transaction.memo,
      createdAt = transaction.createdAt,
    )
