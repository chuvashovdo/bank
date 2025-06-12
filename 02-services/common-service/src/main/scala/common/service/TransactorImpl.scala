package common.service

import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase

class TransactorImpl(quill: Quill.Postgres[SnakeCase]) extends Transactor:
  override def transact[A](effect: Task[A]): Task[A] =
    quill.transaction(effect)

object TransactorImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, Transactor] =
    ZLayer.fromFunction(new TransactorImpl(_))
