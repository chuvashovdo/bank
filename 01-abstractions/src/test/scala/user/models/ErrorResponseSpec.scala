package user.models

import zio.*
import zio.test.*
import zio.json.*

object ErrorResponseSpec extends ZIOSpecDefault:
  def spec =
    suite("ErrorResponse model")(
      test("ErrorResponse should correctly serialize to JSON and back") {
        val errorResponse = ErrorResponse("validation_error")

        val json = errorResponse.toJson
        val parsed = json.fromJson[ErrorResponse]

        assertTrue(parsed.isRight) &&
        assertTrue(parsed.map(_.error).getOrElse("") == "validation_error") &&
        assertTrue(json.contains("validation_error"))
      },
      test("ErrorResponse should handle different error types") {
        val validationError = ErrorResponse("validation_error")
        val authError = ErrorResponse("auth_error")
        val serverError = ErrorResponse("server_error")

        assertTrue(validationError.error == "validation_error") &&
        assertTrue(authError.error == "auth_error") &&
        assertTrue(serverError.error == "server_error")
      },
      test("ErrorResponse with same values should be equal") {
        val error1 = ErrorResponse("same_code")
        val error2 = ErrorResponse("same_code")

        assertTrue(error1.error == error2.error)
      },
      test("ErrorResponse with different values should not be equal") {
        val error1 = ErrorResponse("code1")
        val error2 = ErrorResponse("code2")

        assertTrue(error1.error != error2.error)
      },
    )
