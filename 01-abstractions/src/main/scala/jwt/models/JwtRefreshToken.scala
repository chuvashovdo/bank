package jwt.models

import common.errors.ValueCannotBeEmptyError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import zio.json.{ JsonDecoder, JsonEncoder }

type JwtRefreshTokenConstraint = MinLength[1]
opaque type JwtRefreshToken = String :| JwtRefreshTokenConstraint

object JwtRefreshToken:
  def apply(value: String): Either[ValueCannotBeEmptyError, JwtRefreshToken] =
    value
      .refineEither[JwtRefreshTokenConstraint]
      .left
      .map(_ => ValueCannotBeEmptyError("refresh токен", Some(value)))

  extension (token: JwtRefreshToken) def value: String = token

  given JsonDecoder[JwtRefreshToken] =
    JsonDecoder
      .string
      .mapOrFail(str => JwtRefreshToken.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[JwtRefreshToken] =
    JsonEncoder.string.contramap(_.value)
