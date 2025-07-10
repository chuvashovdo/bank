package user.models

import zio.json.*
import java.util.UUID

opaque type PermissionId = UUID

object PermissionId:
  def apply(uuid: UUID): PermissionId =
    uuid

  def random: PermissionId =
    UUID.randomUUID()

  extension (id: PermissionId) def value: UUID = id

  given CanEqual[PermissionId, PermissionId] =
    CanEqual.derived

  given JsonCodec[PermissionId] =
    JsonCodec.uuid.transform(PermissionId(_), _.value)
