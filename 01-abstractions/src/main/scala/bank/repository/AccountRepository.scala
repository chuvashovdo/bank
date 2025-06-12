package bank.repository

import zio.*
import java.util.UUID
import scala.math.BigDecimal
import bank.models.{ Account, AccountStatus }

trait AccountRepository:
  def create(account: Account): Task[Account]
  def findById(id: UUID): Task[Account]
  def findByAccountNumber(accountNumber: String): Task[Account]
  def findByUserId(userId: UUID): Task[List[Account]]
  def updateBalance(id: UUID, newBalance: BigDecimal): Task[Unit]
  def updateStatus(id: UUID, newStatus: AccountStatus): Task[Unit]
  def delete(id: UUID): Task[Unit]

object AccountRepository:
  def create(account: Account): RIO[AccountRepository, Account] =
    ZIO.serviceWithZIO[AccountRepository](_.create(account))

  def findById(id: UUID): RIO[AccountRepository, Account] =
    ZIO.serviceWithZIO[AccountRepository](_.findById(id))

  def findByAccountNumber(accountNumber: String): RIO[AccountRepository, Account] =
    ZIO.serviceWithZIO[AccountRepository](_.findByAccountNumber(accountNumber))

  def findByUserId(userId: UUID): RIO[AccountRepository, List[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.findByUserId(userId))

  def updateBalance(id: UUID, newBalance: BigDecimal): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.updateBalance(id, newBalance))

  def updateStatus(id: UUID, newStatus: AccountStatus): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.updateStatus(id, newStatus))

  def delete(id: UUID): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(id))
