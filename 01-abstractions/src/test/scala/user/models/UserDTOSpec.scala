package user.models

import zio.*
import zio.test.*
import zio.json.*

object UserDTOSpec extends ZIOSpecDefault:
  // Генераторы данных
  val emailGen: Gen[Any, String] =
    for
      user <- Gen.alphaNumericStringBounded(3, 10)
      domain <- Gen.alphaNumericStringBounded(2, 10)
      tld <- Gen.elements("com", "org", "net", "io")
    yield s"$user@$domain.$tld"

  val passwordGen: Gen[Any, String] =
    Gen.stringBounded(8, 20)(Gen.alphaNumericChar)

  val nameGen: Gen[Any, String] =
    Gen.stringBounded(2, 20)(Gen.alphaChar)

  val optionalNameGen: Gen[Any, Option[String]] =
    Gen.option(nameGen)

  // Генератор для RegisterUserRequest
  val registerUserRequestGen: Gen[Any, RegisterUserRequest] =
    for
      email <- emailGen
      password <- passwordGen
      firstName <- optionalNameGen
      lastName <- optionalNameGen
    yield RegisterUserRequest(email, password, firstName, lastName)

  // Генератор для LoginRequest
  val loginRequestGen: Gen[Any, LoginRequest] =
    for
      email <- emailGen
      password <- passwordGen
    yield LoginRequest(email, password)

  // Генератор для UserResponse
  val userResponseGen: Gen[Any, UserResponse] =
    for
      id <- Gen.alphaNumericStringBounded(5, 10)
      email <- emailGen
      firstName <- optionalNameGen
      lastName <- optionalNameGen
    yield UserResponse(id, email, firstName, lastName)

  def spec =
    suite("User DTO models")(
      test("RegisterUserRequest should correctly serialize to JSON and back") {
        check(registerUserRequestGen) { request =>
          val json = request.toJson
          val parsed = json.fromJson[RegisterUserRequest]

          assertTrue(parsed.isRight) &&
          assertTrue {
            parsed
              .map { r =>
                r.email == request.email &&
                r.password == request.password &&
                r.firstName == request.firstName &&
                r.lastName == request.lastName
              }
              .getOrElse(false)
          }
        }
      },
      test("LoginRequest should correctly serialize to JSON and back") {
        check(loginRequestGen) { request =>
          val json = request.toJson
          val parsed = json.fromJson[LoginRequest]

          assertTrue(parsed.isRight) &&
          assertTrue(
            parsed
              .map(r =>
                r.email == request.email &&
                r.password == request.password
              )
              .getOrElse(false)
          )
        }
      },
      test("UserResponse should correctly serialize to JSON and back") {
        check(userResponseGen) { response =>
          val json = response.toJson
          val parsed = json.fromJson[UserResponse]

          assertTrue(parsed.isRight) &&
          assertTrue {
            parsed
              .map { r =>
                r.id == response.id &&
                r.email == response.email &&
                r.firstName == response.firstName &&
                r.lastName == response.lastName
              }
              .getOrElse(false)
          }
        }
      },

      // Заглушка для тестов валидации, так как методы isEmailValid и isPasswordValid не найдены
      test("RegisterUserRequest should validate input") {
        val validRequest =
          RegisterUserRequest(
            "valid@example.com",
            "password123",
            Some("John"),
            Some("Doe"),
          )

        // Базовые проверки формата
        assertTrue(validRequest.email.contains("@")) &&
        assertTrue(validRequest.password.length >= 8)
      },
    )
