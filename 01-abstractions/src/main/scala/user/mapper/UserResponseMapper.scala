package user.mapper

import user.models.*
import zio.*

trait UserResponseMapper:
  def fromUser(user: User): Task[UserResponse]

object UserResponseMapper:
  def fromUser(user: User): RIO[UserResponseMapper, UserResponse] =
    ZIO.serviceWithZIO[UserResponseMapper](_.fromUser(user))
