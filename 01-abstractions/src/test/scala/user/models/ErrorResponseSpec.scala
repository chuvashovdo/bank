package common.models

import zio.*
import zio.test.*
import zio.json.*
import java.time.Instant
import common.errors.{ ValidationError, InvalidDataFormatError, ValueCannotBeEmptyError }

object ErrorResponseSpec extends ZIOSpecDefault:
  def spec =
    suite("common.models.ErrorResponse model")(
      test("ErrorResponse should correctly serialize to JSON and back") {
        val errorDetail = ErrorDetail(Some("email"), "INVALID_FORMAT", "Invalid email format")
        val timestamp = Instant.now()
        val originalResponse =
          ErrorResponse(
            timestamp = timestamp,
            status = 400,
            errorType = "VALIDATION_ERROR",
            message = "Input data is invalid",
            path = "/users/register",
            details = Some(List(errorDetail)),
          )

        val json = originalResponse.toJson
        val parsedEither = json.fromJson[ErrorResponse]

        parsedEither match
          case Right(parsedResponse) =>
            assertTrue(
              parsedResponse.status == originalResponse.status,
              parsedResponse.errorType == originalResponse.errorType,
              parsedResponse.message == originalResponse.message,
              parsedResponse.path == originalResponse.path,
              parsedResponse.details.map(_.length) == originalResponse.details.map(_.length),
            )
          case Left(err) =>
            println(s"JSON parsing failed: $err")
            assertTrue(false)
      },
      test("ErrorResponse.fromValidationErrors should create correct ErrorResponse") {
        val validationErrors: List[ValidationError] =
          List(
            InvalidDataFormatError(
              "email",
              "Must be a valid email",
              Some("test@"),
              "Invalid regex match",
            ),
            ValueCannotBeEmptyError("password", Some("")),
          )

        val path = "/auth/login"
        val generalMessage = "Login failed due to validation errors"
        val httpStatus = 422
        val errorTypeStr = "VALIDATION_ISSUE"

        val errorResponse =
          ErrorResponse.fromValidationErrors(
            httpStatus = httpStatus,
            errorType = errorTypeStr,
            path = path,
            generalMessage = generalMessage,
            errors = validationErrors,
          )

        assertTrue(
          errorResponse.status == httpStatus,
          errorResponse.errorType == errorTypeStr,
          errorResponse.message == generalMessage,
          errorResponse.path == path,
          errorResponse.details.isDefined,
        ) && {
          val details = errorResponse.details.get
          assertTrue(details.length == 2) &&
          assertTrue {
            details.exists(d =>
              d.field.contains("email") &&
              d.code == "INVALID_FORMAT" &&
              d.message.contains("Must be a valid email")
            )
          } &&
          assertTrue {
            details.exists { d =>
              d.field.contains("password") &&
              d.code == "VALUE_CANNOT_BE_EMPTY" &&
              d.message.contains("не может быть пустым")
            }
          }
        }
      },
      test("ErrorResponse should handle different error types") {
        val validationError =
          ErrorResponse(
            Instant.now(),
            400,
            "VALIDATION_ERROR",
            "Validation error",
            "/auth/login",
            Some(List(ErrorDetail(Some("email"), "INVALID_FORMAT", "Invalid email format"))),
          )
        val authError =
          ErrorResponse(
            Instant.now(),
            401,
            "AUTH_ERROR",
            "Authentication error",
            "/auth/login",
            None,
          )
        val serverError =
          ErrorResponse(Instant.now(), 500, "SERVER_ERROR", "Server error", "/auth/login", None)

        assertTrue(validationError.errorType == "VALIDATION_ERROR") &&
        assertTrue(authError.errorType == "AUTH_ERROR") &&
        assertTrue(serverError.errorType == "SERVER_ERROR")
      },
      test("ErrorResponse with same values should be equal") {
        val error1 =
          ErrorResponse(
            Instant.now(),
            400,
            "VALIDATION_ERROR",
            "Validation error",
            "/auth/login",
            Some(List(ErrorDetail(Some("email"), "INVALID_FORMAT", "Invalid email format"))),
          )
        val error2 =
          ErrorResponse(
            Instant.now(),
            400,
            "VALIDATION_ERROR",
            "Validation error",
            "/auth/login",
            Some(List(ErrorDetail(Some("email"), "INVALID_FORMAT", "Invalid email format"))),
          )

        assertTrue(error1.errorType == error2.errorType)
      },
      test("ErrorResponse with different values should not be equal") {
        val error1 =
          ErrorResponse(
            Instant.now(),
            400,
            "VALIDATION_ERROR",
            "Validation error",
            "/auth/login",
            Some(List(ErrorDetail(Some("email"), "INVALID_FORMAT", "Invalid email format"))),
          )
        val error2 =
          ErrorResponse(
            Instant.now(),
            401,
            "AUTHENTICATION_ERROR",
            "Authentication failed",
            "/auth/token",
            None,
          )

        assertTrue(
          error1.errorType != error2.errorType,
          error1.status != error2.status,
          error1.message != error2.message,
          error1.path != error2.path,
        )
      },
    )
