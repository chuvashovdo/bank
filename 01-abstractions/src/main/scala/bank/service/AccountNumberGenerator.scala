package bank.service

import zio.*

trait AccountNumberGenerator:
  def generate: UIO[String]

object AccountNumberGenerator:
  def generate: URIO[AccountNumberGenerator, String] =
    ZIO.serviceWithZIO[AccountNumberGenerator](_.generate)
