package user.models

import common.errors.InvalidDataFormatError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.{ MinLength, MaxLength }
import zio.json.{ JsonDecoder, JsonEncoder }

type PasswordConstraint = MinLength[8] & MaxLength[128]

opaque type Password = String :| PasswordConstraint

object Password:
  def apply(value: String): Either[InvalidDataFormatError, Password] =
    value
      .refineEither[PasswordConstraint]
      .left
      .map { ironMsg =>
        InvalidDataFormatError(
          fieldName = "password",
          ruleDescription = "длина должна быть от 8 до 128 символов",
          input = None,
          details = ironMsg,
        )
      }

  extension (password: Password) def value: String = password

  given JsonDecoder[Password] =
    JsonDecoder.string.mapOrFail(str => Password.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[Password] =
    JsonEncoder.string.contramap(_.value)
