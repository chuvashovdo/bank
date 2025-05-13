package common.types

import common.errors.ValueCannotBeEmptyError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import zio.json.{ JsonDecoder, JsonEncoder }

opaque type NonEmptyString = String :| MinLength[1]

object NonEmptyString:
  def apply(value: String): Either[ValueCannotBeEmptyError, NonEmptyString] =
    value
      .refineEither[MinLength[1]]
      .left
      .map(_ => ValueCannotBeEmptyError("Строка не может быть пустой", Some(value)))

  extension (nes: NonEmptyString) def value: String = nes

  given JsonDecoder[NonEmptyString] =
    JsonDecoder
      .string
      .mapOrFail(str => NonEmptyString.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[NonEmptyString] =
    JsonEncoder.string.contramap(_.value)
