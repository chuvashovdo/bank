package user.models

import zio.json.*

final case class ErrorResponse(
  error: String
)

object ErrorResponse:
  given JsonCodec[ErrorResponse] =
    DeriveJsonCodec.gen[ErrorResponse]

  // Ошибки
  case class Unauthorized() extends Exception
  case class NotFound() extends Exception
  case class Conflict() extends Exception

// ! TODO: change it to better error handling
