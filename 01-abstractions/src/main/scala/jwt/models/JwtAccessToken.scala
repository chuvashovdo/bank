package jwt.models

import common.errors.ValueCannotBeEmptyError
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.MinLength
import zio.json.{ JsonDecoder, JsonEncoder }

type JwtAccessTokenConstraint = MinLength[1]
opaque type JwtAccessToken = String :| JwtAccessTokenConstraint

object JwtAccessToken:
  def apply(value: String): Either[ValueCannotBeEmptyError, JwtAccessToken] =
    value
      .refineEither[JwtAccessTokenConstraint]
      .left
      .map(_ => ValueCannotBeEmptyError("access токен", Some(value)))

  extension (token: JwtAccessToken) def value: String = token

  given JsonDecoder[JwtAccessToken] =
    JsonDecoder
      .string
      .mapOrFail(str => JwtAccessToken.apply(str).left.map(_.developerFriendlyMessage))
  given JsonEncoder[JwtAccessToken] =
    JsonEncoder.string.contramap(_.value)
