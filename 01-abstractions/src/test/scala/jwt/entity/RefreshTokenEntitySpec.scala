package jwt.entity

import zio.*
import zio.test.*
import java.time.Instant

object RefreshTokenEntitySpec extends ZIOSpecDefault:
  def spec =
    suite("RefreshTokenEntity")(
      test("RefreshTokenEntity should properly initialize and store values") {
        val now = Instant.now()
        val id = "token-id-123"
        val userId = "user-123"
        val refreshToken = "jwt-token-string"
        val expiresAt = now.plusSeconds(86400) // +24 часа

        val entity = RefreshTokenEntity(id, userId, refreshToken, expiresAt, now)

        assertTrue(entity.id == id) &&
        assertTrue(entity.userId == userId) &&
        assertTrue(entity.refreshToken == refreshToken) &&
        assertTrue(entity.expiresAt.equals(expiresAt)) &&
        assertTrue(entity.createdAt.equals(now))
      },
      test("Entity should check expiration correctly") {
        val now = Instant.now()
        val pastTime = now.minusSeconds(3600) // -1 час
        val futureTime = now.plusSeconds(3600) // +1 час

        val expiredEntity =
          RefreshTokenEntity(
            "id1",
            "user1",
            "token1",
            pastTime, // уже истек
            now.minusSeconds(7200), // создан 2 часа назад
          )

        val validEntity =
          RefreshTokenEntity(
            "id2",
            "user1",
            "token2",
            futureTime, // еще не истек
            now,
          )

        assertTrue(expiredEntity.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validEntity.expiresAt.isAfter(Instant.now()))
      },
      test("Entities with same values should compare fields") {
        val now = Instant.now()
        val entity1 = RefreshTokenEntity("same-id", "same-user", "same-token", now, now)
        val entity2 = RefreshTokenEntity("same-id", "same-user", "same-token", now, now)

        assertTrue(entity1.id == entity2.id) &&
        assertTrue(entity1.userId == entity2.userId) &&
        assertTrue(entity1.refreshToken == entity2.refreshToken) &&
        assertTrue(entity1.expiresAt.equals(entity2.expiresAt)) &&
        assertTrue(entity1.createdAt.equals(entity2.createdAt))
      },
      test("Entities with different values should have different fields") {
        val now = Instant.now()
        val entity1 = RefreshTokenEntity("id1", "user1", "token1", now, now)
        val entity2 = RefreshTokenEntity("id2", "user2", "token2", now, now)

        assertTrue(entity1.id != entity2.id) &&
        assertTrue(entity1.userId != entity2.userId) &&
        assertTrue(entity1.refreshToken != entity2.refreshToken)
      },
    )
