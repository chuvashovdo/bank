package bank.models

import zio.json.*
import scala.CanEqual.derived

enum AccountStatus:
  case OPEN, CLOSED, FROZEN

object AccountStatus:
  given JsonCodec[AccountStatus] =
    DeriveJsonCodec.gen[AccountStatus]

  given CanEqual[AccountStatus, AccountStatus] =
    derived
