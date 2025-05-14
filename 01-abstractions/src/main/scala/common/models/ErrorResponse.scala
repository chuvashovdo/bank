package common.models

import zio.json.*
import java.time.Instant
import common.errors.*

final case class ErrorDetail(
  field: Option[String],
  code: String,
  message: String,
)

object ErrorDetail:
  given JsonCodec[ErrorDetail] =
    DeriveJsonCodec.gen[ErrorDetail]

final case class ErrorResponse(
  timestamp: Instant,
  status: Int,
  errorType: String,
  message: String,
  path: String,
  details: Option[List[ErrorDetail]],
)

object ErrorResponse:
  given JsonCodec[ErrorResponse] =
    DeriveJsonCodec.gen[ErrorResponse]

  def fromValidationErrors(
    httpStatus: Int,
    errorType: String,
    path: String,
    generalMessage: String,
    errors: List[ValidationError],
  ): ErrorResponse =
    val errorDetails =
      errors.map { ve =>
        val (fieldName, specificCode) =
          ve match
            case e: InvalidDataFormatError => (Some(e.fieldName), "INVALID_FORMAT")
            case e: ValueCannotBeEmptyError => (Some(e.fieldName), "VALUE_CANNOT_BE_EMPTY")
        ErrorDetail(
          field = fieldName,
          code = specificCode,
          message = ve.developerFriendlyMessage,
        )
      }
    ErrorResponse(
      timestamp = Instant.now(),
      status = httpStatus,
      errorType = errorType,
      message = generalMessage,
      path = path,
      details = Some(errorDetails).filter(_.nonEmpty),
    )

  def fromBusinessError(
    httpStatus: Int,
    path: String,
    error: BusinessError,
  ): ErrorResponse =
    ErrorResponse(
      timestamp = Instant.now(),
      status = httpStatus,
      errorType = error.errorCode,
      message = error.message,
      path = path,
      details = None,
    )
