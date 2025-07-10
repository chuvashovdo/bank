package user.models

import common.errors.InvalidDataFormatError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.string.Match
import zio.json.{ JsonDecoder, JsonEncoder }

inline private val EmailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"""

opaque type Email = String :| Match[EmailRegex.type]

object Email:
  def apply(value: String): Either[InvalidDataFormatError, Email] =
    value
      .refineEither[Match[EmailRegex.type]]
      .left
      .map { ironMsg =>
        InvalidDataFormatError(
          fieldName = "email",
          ruleDescription = "должен быть валидным email адресом",
          input = Some(value),
          details = ironMsg,
        )
      }

  extension (email: Email) def value: String = email

  given JsonDecoder[Email] =
    JsonDecoder.string.mapOrFail(str => Email.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[Email] =
    JsonEncoder.string.contramap(_.value)
