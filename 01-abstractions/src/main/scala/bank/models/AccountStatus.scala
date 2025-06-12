package bank.models

import zio.json.*

enum AccountStatus:
  case OPEN, CLOSED, FROZEN

object AccountStatus:
  given JsonCodec[AccountStatus] =
    DeriveJsonCodec.gen[AccountStatus]
