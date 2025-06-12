package user.models

import zio.json.*
import java.util.UUID

opaque type UserId = UUID

object UserId:
  def apply(uuid: UUID): UserId =
    uuid

  def random: UserId =
    UUID.randomUUID()

  extension (id: UserId) def value: UUID = id

  given CanEqual[UserId, UserId] =
    CanEqual.derived

  given JsonCodec[UserId] =
    JsonCodec.uuid.transform(UserId(_), _.value)
