package common.models

import zio.json.*
import java.time.Instant
import common.errors.ValidationError

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
            case e: common.errors.InvalidDataFormatError => (Some(e.fieldName), "INVALID_FORMAT")
            case e: common.errors.ValueCannotBeEmptyError =>
              (Some(e.fieldName), "VALUE_CANNOT_BE_EMPTY")
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
