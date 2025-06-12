package bank.service

import zio.*

class AccountNumberGeneratorImpl extends AccountNumberGenerator:
  private val charSet =
    ('A' to 'Z') ++ ('0' to '9')
  override def generate: UIO[String] =
    ZIO
      .collectAll(
        List.fill(12)(Random.nextIntBounded(charSet.length).map(charSet))
      )
      .map(chars => s"ACC${chars.mkString}")

object AccountNumberGeneratorImpl:
  val layer: ULayer[AccountNumberGenerator] =
    ZLayer.succeed(new AccountNumberGeneratorImpl)
