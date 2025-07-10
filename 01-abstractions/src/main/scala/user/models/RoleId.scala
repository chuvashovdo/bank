package user.models

import zio.json.*
import java.util.UUID

opaque type RoleId = UUID

object RoleId:
  def apply(uuid: UUID): RoleId =
    uuid

  def random: RoleId =
    UUID.randomUUID()

  extension (id: RoleId) def value: UUID = id

  given CanEqual[RoleId, RoleId] =
    CanEqual.derived

  given JsonCodec[RoleId] =
    JsonCodec.uuid.transform(RoleId(_), _.value)
