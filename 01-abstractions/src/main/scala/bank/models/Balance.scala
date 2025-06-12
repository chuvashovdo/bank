package bank.models

import common.errors.InvalidDataFormatError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive0
import zio.json.{ JsonDecoder, JsonEncoder }
import scala.math.BigDecimal

given Constraint[BigDecimal, Positive0]:
  inline override def test(inline value: BigDecimal): Boolean =
    value >= BigDecimal(0)
  inline override def message: String =
    "Should be positive or zero"

opaque type Balance = BigDecimal :| Positive0

object Balance:
  def apply(value: BigDecimal): Either[InvalidDataFormatError, Balance] =
    value
      .refineEither[Positive0]
      .left
      .map { ironMsg =>
        InvalidDataFormatError(
          fieldName = "balance",
          ruleDescription = "баланс не может быть отрицательным",
          input = Some(value.toString),
          details = ironMsg,
        )
      }

  val zero: Balance =
    BigDecimal(0).refineUnsafe[Positive0]

  extension (balance: Balance) def value: BigDecimal = balance

  given JsonDecoder[Balance] =
    JsonDecoder.scalaBigDecimal.mapOrFail(bd => Balance(bd).left.map(_.developerFriendlyMessage))
  given JsonEncoder[Balance] =
    JsonEncoder.scalaBigDecimal.contramap(_.value)
