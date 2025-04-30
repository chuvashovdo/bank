package user.models

import zio.json.*

final case class UserId(value: String) extends AnyVal

object UserId:
  given JsonCodec[UserId] =
    JsonCodec
      .string
      .transform(
        string => UserId(string),
        userId => userId.value,
      )
