package user.mapper

import user.models.*
import zio.*

class UserResponseMapperImpl extends UserResponseMapper:
  override def fromUser(user: User): Task[UserResponse] =
    ZIO.succeed(UserResponse(user.id.value, user.email, user.firstName, user.lastName))

object UserResponseMapperImpl:
  val layer: ULayer[UserResponseMapper] =
    ZLayer.succeed(UserResponseMapperImpl())
