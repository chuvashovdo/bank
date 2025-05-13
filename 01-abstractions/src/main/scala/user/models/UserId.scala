package user.models

import zio.json.*
import common.errors.{ ValidationError, ValueCannotBeEmptyError }
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.*

opaque type UserId = String

object UserId:
  def apply(value: String): Either[ValidationError, UserId] =
    value.refineEither[MinLength[1]] match
      case Left(errorMessage) => Left(ValueCannotBeEmptyError("UserId", Some(value)))
      case Right(refinedValue) => Right(refinedValue)

  def unsafe(value: String): UserId =
    value.refineUnsafe[MinLength[1]]

  extension (id: UserId) def value: String = id

  given CanEqual[UserId, UserId] =
    CanEqual.derived

  given JsonCodec[UserId] =
    JsonCodec
      .string
      .transformOrFail(
        str => UserId(str).left.map(_.developerFriendlyMessage),
        uid => uid.value,
      )
