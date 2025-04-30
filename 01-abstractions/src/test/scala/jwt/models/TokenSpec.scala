package jwt.models

import zio.*
import zio.test.*
import user.models.UserId
import java.time.Instant

object TokenSpec extends ZIOSpecDefault:
  // Тестовые данные
  val userId =
    UserId("test-user-123")
  val token =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXItMTIzIiwiaWF0IjoxNTE2MjM5MDIyfQ.emnWLgvWMvCQJIJQ8In6Dz0nL_gk75zkxkhI53vvLQA"
  val expiresAt =
    Instant.now().plusSeconds(3600) // +1 час

  def spec =
    suite("JWT Token models")(
      test("AccessToken should correctly store values") {
        val accessToken = AccessToken(token, expiresAt, userId)

        assertTrue(accessToken.token == token) &&
        assertTrue(accessToken.expiresAt.equals(expiresAt)) &&
        assertTrue(accessToken.userId.value == userId.value)
      },
      test("RefreshToken should correctly store values") {
        val refreshToken = RefreshToken(token, expiresAt, userId)

        assertTrue(refreshToken.token == token) &&
        assertTrue(refreshToken.expiresAt.equals(expiresAt)) &&
        assertTrue(refreshToken.userId.value == userId.value)
      },
      test("AccessToken should check expiration correctly") {
        val expiredToken =
          AccessToken(
            token,
            Instant.now().minusSeconds(3600), // -1 час (уже истек)
            userId,
          )

        val validToken =
          AccessToken(
            token,
            Instant.now().plusSeconds(3600), // +1 час (еще действителен)
            userId,
          )

        assertTrue(expiredToken.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validToken.expiresAt.isAfter(Instant.now()))
      },
      test("RefreshToken should check expiration correctly") {
        val expiredToken =
          RefreshToken(
            token,
            Instant.now().minusSeconds(3600), // -1 час (уже истек)
            userId,
          )

        val validToken =
          RefreshToken(
            token,
            Instant.now().plusSeconds(3600), // +1 час (еще действителен)
            userId,
          )

        assertTrue(expiredToken.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validToken.expiresAt.isAfter(Instant.now()))
      },
      test("Tokens with same values should compare fields") {
        val token1 = AccessToken("same-token", expiresAt, userId)
        val token2 = AccessToken("same-token", expiresAt, userId)

        val refreshToken1 = RefreshToken("same-refresh", expiresAt, userId)
        val refreshToken2 = RefreshToken("same-refresh", expiresAt, userId)

        assertTrue(token1.token == token2.token) &&
        assertTrue(token1.expiresAt.equals(token2.expiresAt)) &&
        assertTrue(token1.userId.value == token2.userId.value) &&
        assertTrue(refreshToken1.token == refreshToken2.token) &&
        assertTrue(refreshToken1.expiresAt.equals(refreshToken2.expiresAt)) &&
        assertTrue(refreshToken1.userId.value == refreshToken2.userId.value)
      },
      test("Tokens with different values should have different fields") {
        val token1 = AccessToken("token1", expiresAt, userId)
        val token2 = AccessToken("token2", expiresAt, userId)

        val refreshToken1 = RefreshToken("refresh1", expiresAt, userId)
        val refreshToken2 = RefreshToken("refresh2", expiresAt, userId)

        assertTrue(token1.token != token2.token) &&
        assertTrue(refreshToken1.token != refreshToken2.token)
      },
    )
