package user.models

import common.errors.ValueCannotBeEmptyError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import zio.json.{ JsonDecoder, JsonEncoder }

type LastNameConstraint = MinLength[1]
opaque type LastName = String :| LastNameConstraint

object LastName:
  def apply(value: String): Either[ValueCannotBeEmptyError, LastName] =
    value
      .refineEither[LastNameConstraint]
      .left
      .map(_ => ValueCannotBeEmptyError("фамилия", Some(value)))

  extension (lastName: LastName) def value: String = lastName

  given JsonDecoder[LastName] =
    JsonDecoder.string.mapOrFail(str => LastName.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[LastName] =
    JsonEncoder.string.contramap(_.value)
