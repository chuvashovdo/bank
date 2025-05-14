package user.models

import common.errors.ValueCannotBeEmptyError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import zio.json.{ JsonDecoder, JsonEncoder }

type FirstNameConstraint = MinLength[1]
opaque type FirstName = String :| FirstNameConstraint

object FirstName:
  def apply(value: String): Either[ValueCannotBeEmptyError, FirstName] =
    value
      .refineEither[FirstNameConstraint]
      .left
      .map(_ => ValueCannotBeEmptyError("имя", Some(value)))

  extension (firstName: FirstName) def value: String = firstName

  given JsonDecoder[FirstName] =
    JsonDecoder.string.mapOrFail(str => FirstName.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[FirstName] =
    JsonEncoder.string.contramap(_.value)
