package bank.models

import zio.json.*
import java.util.UUID
import scala.CanEqual.derived

opaque type TransactionId = UUID

object TransactionId:
  def apply(uuid: UUID): TransactionId =
    uuid
  def random: TransactionId =
    UUID.randomUUID()

  extension (id: TransactionId) def value: UUID = id

  given CanEqual[TransactionId, TransactionId] =
    derived
  given JsonCodec[TransactionId] =
    JsonCodec.uuid.transform(TransactionId(_), _.value)
