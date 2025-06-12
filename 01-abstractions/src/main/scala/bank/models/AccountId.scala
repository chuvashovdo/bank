package bank.models

import zio.json.*
import java.util.UUID
import scala.CanEqual.derived

opaque type AccountId = UUID

object AccountId:
  def apply(uuid: UUID): AccountId =
    uuid
  def random: AccountId =
    UUID.randomUUID()

  extension (id: AccountId) def value: UUID = id

  given CanEqual[AccountId, AccountId] =
    derived
  given JsonCodec[AccountId] =
    JsonCodec.uuid.transform(AccountId(_), _.value)
