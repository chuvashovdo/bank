package user.models.dto

import zio.*
import zio.test.*
import zio.json.*
import common.errors.{ InvalidDataFormatError, ValueCannotBeEmptyError }
import user.models.*

object UserDTOSpec extends ZIOSpecDefault:
  // Генераторы данных
  val rawEmailStringGen: Gen[Any, String] =
    for
      user <- Gen.alphaNumericStringBounded(3, 10)
      domain <- Gen.alphaNumericStringBounded(2, 10)
      tld <- Gen.elements("com", "org", "net", "io")
    yield s"$user@$domain.$tld"

  val emailGen: Gen[Any, Email] =
    rawEmailStringGen.map(Email.apply).collect { case Right(email) => email }

  val rawPasswordStringGen: Gen[Any, String] =
    Gen.stringBounded(8, 20)(Gen.alphaNumericChar)

  val passwordGen: Gen[Any, Password] =
    rawPasswordStringGen.map(Password.apply).collect { case Right(password) => password }

  val rawNameStringGen: Gen[Any, String] =
    Gen.stringBounded(2, 20)(Gen.alphaChar)

  val firstNameGen: Gen[Any, FirstName] =
    rawNameStringGen.map(FirstName.apply).collect { case Right(firstName) => firstName }

  val lastNameGen: Gen[Any, LastName] =
    rawNameStringGen.map(LastName.apply).collect { case Right(lastName) => lastName }

  val optionalFirstNameGen: Gen[Any, Option[FirstName]] =
    Gen.option(firstNameGen)

  val optionalLastNameGen: Gen[Any, Option[LastName]] =
    Gen.option(lastNameGen)

  val userIdGen: Gen[Any, UserId] =
    Gen.const(UserId.random)

  // Генератор для RegisterUserRequest
  val registerUserRequestGen: Gen[Any, RegisterUserRequest] =
    for
      email <- emailGen
      password <- passwordGen
      firstName <- optionalFirstNameGen
      lastName <- optionalLastNameGen
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
      email <- emailGen
      firstName <- optionalFirstNameGen
      lastName <- optionalLastNameGen
      userId <- userIdGen
    yield UserResponse(userId, email, firstName, lastName)

  def spec =
    suite("User DTO models")(
      test("RegisterUserRequest should correctly serialize to JSON and back") {
        check(registerUserRequestGen) { request =>
          val json = request.toJson
          val parsed = json.fromJson[RegisterUserRequest]

          assertTrue(parsed.isRight) &&
          assertTrue(parsed.toOption.contains(request))
        }
      },
      test("LoginRequest should correctly serialize to JSON and back") {
        check(loginRequestGen) { request =>
          val json = request.toJson
          val parsed = json.fromJson[LoginRequest]

          assertTrue(parsed.isRight) &&
          assertTrue(parsed.toOption.contains(request))
        }
      },
      test("UserResponse should correctly serialize to JSON and back") {
        check(userResponseGen) { response =>
          val json = response.toJson
          val parsed = json.fromJson[UserResponse]

          assertTrue(parsed.isRight) &&
          assertTrue(parsed.toOption.contains(response))
        }
      },
    )
