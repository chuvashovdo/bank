package bank.api

import jwt.service.JwtService
import zio.*
import sttp.tapir.server.ServerEndpoint
import bank.service.AccountService
import bank.service.TransactionService

class BankApi(
  accountService: AccountService,
  transactionService: TransactionService,
  jwtService: JwtService,
):
  private val accountEndpoints =
    new AccountEndpoints(
      accountService,
      jwtService,
    )

  private val transactionEndpoints =
    new TransactionEndpoints(
      transactionService,
      jwtService,
    )

  val allEndpoints: List[ServerEndpoint[Any, Task]] =
    accountEndpoints.all ++ transactionEndpoints.all

object BankApi:
  val layer: ZLayer[AccountService & TransactionService & JwtService, Nothing, BankApi] =
    ZLayer:
      for
        accountService <- ZIO.service[AccountService]
        transactionService <- ZIO.service[TransactionService]
        jwtService <- ZIO.service[JwtService]
      yield new BankApi(accountService, transactionService, jwtService)
