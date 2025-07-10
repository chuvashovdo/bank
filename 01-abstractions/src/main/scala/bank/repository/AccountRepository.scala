package bank.repository

import zio.*
import scala.math.BigDecimal
import bank.models.AccountStatus
import java.util.UUID
import bank.entity.AccountEntity

trait AccountRepository:
  def create(account: AccountEntity): Task[AccountEntity]
  def findById(id: UUID): Task[AccountEntity]
  def findByAccountNumber(accountNumber: String): Task[AccountEntity]
  def findByUserId(userId: UUID): Task[List[AccountEntity]]
  def findAll(): Task[List[AccountEntity]]
  def updateBalance(id: UUID, newBalance: BigDecimal): Task[Unit]
  def updateStatus(id: UUID, newStatus: AccountStatus): Task[Unit]
  def delete(id: UUID): Task[Unit]
  def getNextAccountNumber: Task[Long]

object AccountRepository:
  def create(account: AccountEntity): RIO[AccountRepository, AccountEntity] =
    ZIO.serviceWithZIO[AccountRepository](_.create(account))

  def findById(id: UUID): RIO[AccountRepository, AccountEntity] =
    ZIO.serviceWithZIO[AccountRepository](_.findById(id))

  def findByAccountNumber(accountNumber: String): RIO[AccountRepository, AccountEntity] =
    ZIO.serviceWithZIO[AccountRepository](_.findByAccountNumber(accountNumber))

  def findByUserId(userId: UUID): RIO[AccountRepository, List[AccountEntity]] =
    ZIO.serviceWithZIO[AccountRepository](_.findByUserId(userId))

  def findAll(): RIO[AccountRepository, List[AccountEntity]] =
    ZIO.serviceWithZIO[AccountRepository](_.findAll())

  def updateBalance(id: UUID, newBalance: BigDecimal): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.updateBalance(id, newBalance))

  def updateStatus(id: UUID, newStatus: AccountStatus): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.updateStatus(id, newStatus))

  def delete(id: UUID): RIO[AccountRepository, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(id))

  def getNextAccountNumber: RIO[AccountRepository, Long] =
    ZIO.serviceWithZIO[AccountRepository](_.getNextAccountNumber)
