package jwt.models

import zio.*
import zio.test.*
import user.models.UserId
import java.time.Instant

object TokenSpec extends ZIOSpecDefault:
  // Хелпер для создания валидных значений в тестах (можно вынести в общий файл)
  private def valid[E, A](either: Either[E, A], fieldName: String): A =
    either.fold(
      e => throw new RuntimeException(s"Failed to create valid $fieldName for test: $e"),
      identity,
    )

  // Тестовые данные
  val testUserId =
    UserId.random

  val rawTokenString =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXItMTIzIiwiaWF0IjoxNTE2MjM5MDIyfQ.emnWLgvWMvCQJIJQ8In6Dz0nL_gk75zkxkhI53vvLQA"

  val testJwtAccessToken =
    valid(JwtAccessToken(rawTokenString), "JwtAccessToken")
  val testJwtRefreshToken =
    valid(JwtRefreshToken(rawTokenString), "JwtRefreshToken")

  val expiresAtInstant: Instant =
    Instant.now().plusSeconds(3600) // +1 час

  def spec =
    suite("JWT Token models")(
      test("AccessToken should correctly store values") {
        val accessToken = AccessToken(testJwtAccessToken, expiresAtInstant, testUserId)

        assertTrue(accessToken.token.value == rawTokenString) &&
        assertTrue(accessToken.expiresAt.equals(expiresAtInstant)) &&
        assertTrue(accessToken.userId.equals(testUserId))
      },
      test("RefreshToken should correctly store values") {
        val refreshToken = RefreshToken(testJwtRefreshToken, expiresAtInstant, testUserId)

        assertTrue(refreshToken.token.value == rawTokenString) &&
        assertTrue(refreshToken.expiresAt.equals(expiresAtInstant)) &&
        assertTrue(refreshToken.userId.equals(testUserId))
      },
      test("AccessToken should check expiration correctly") {
        val expiredTime = Instant.now().minusSeconds(3600)
        val validTime = Instant.now().plusSeconds(3600)

        val expiredToken =
          AccessToken(
            testJwtAccessToken,
            expiredTime,
            testUserId,
          )

        val validToken =
          AccessToken(
            testJwtAccessToken,
            validTime,
            testUserId,
          )

        assertTrue(expiredToken.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validToken.expiresAt.isAfter(Instant.now()))
      },
      test("RefreshToken should check expiration correctly") {
        val expiredTime = Instant.now().minusSeconds(3600)
        val validTime = Instant.now().plusSeconds(3600)

        val expiredToken =
          RefreshToken(
            testJwtRefreshToken,
            expiredTime,
            testUserId,
          )

        val validToken =
          RefreshToken(
            testJwtRefreshToken,
            validTime,
            testUserId,
          )

        assertTrue(expiredToken.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validToken.expiresAt.isAfter(Instant.now()))
      },
      test("Tokens with same values should compare fields") {
        val tokenStr1 = "same-token-string"
        val jwtAccess1 = valid(JwtAccessToken(tokenStr1), "JwtAccess1")
        val jwtRefresh1 = valid(JwtRefreshToken(tokenStr1), "JwtRefresh1")
        val userForTest1 = UserId.random
        val expires1 = Instant.now().plusSeconds(1000)

        val accessToken1 = AccessToken(jwtAccess1, expires1, userForTest1)
        val accessToken2 = AccessToken(jwtAccess1, expires1, userForTest1)

        val refreshToken1 = RefreshToken(jwtRefresh1, expires1, userForTest1)
        val refreshToken2 = RefreshToken(jwtRefresh1, expires1, userForTest1)

        // Сравниваем поля индивидуально
        assertTrue(accessToken1.token.value == accessToken2.token.value) &&
        assertTrue(accessToken1.expiresAt.equals(accessToken2.expiresAt)) &&
        assertTrue(accessToken1.userId.equals(accessToken2.userId)) &&
        assertTrue(refreshToken1.token.value == refreshToken2.token.value) &&
        assertTrue(refreshToken1.expiresAt.equals(refreshToken2.expiresAt)) &&
        assertTrue(refreshToken1.userId.equals(refreshToken2.userId))
      },
      test("Tokens with different values should have different fields") {
        val tokenStr_A = "tokenA"
        val tokenStr_B = "tokenB"
        val jwtAccess_A = valid(JwtAccessToken(tokenStr_A), "JwtAccessA")
        val jwtAccess_B = valid(JwtAccessToken(tokenStr_B), "JwtAccessB")
        val jwtRefresh_A = valid(JwtRefreshToken(tokenStr_A), "JwtRefreshA")
        val jwtRefresh_B = valid(JwtRefreshToken(tokenStr_B), "JwtRefreshB")
        val user_A = UserId.random

        val accessToken_A = AccessToken(jwtAccess_A, expiresAtInstant, user_A)
        val accessToken_B = AccessToken(jwtAccess_B, expiresAtInstant, user_A)

        val refreshToken_A = RefreshToken(jwtRefresh_A, expiresAtInstant, user_A)
        val refreshToken_B = RefreshToken(jwtRefresh_B, expiresAtInstant, user_A)

        assertTrue(accessToken_A.token.value != accessToken_B.token.value) &&
        assertTrue(refreshToken_A.token.value != refreshToken_B.token.value)
      },
    )
