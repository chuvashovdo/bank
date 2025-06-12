package common.service

import zio.Task
import zio.ZIO

trait Transactor:
  def transact[A](effect: Task[A]): Task[A]

object Transactor:
  def transact[A](effect: Task[A]): ZIO[Transactor, Throwable, A] =
    ZIO.serviceWithZIO[Transactor](_.transact(effect))
