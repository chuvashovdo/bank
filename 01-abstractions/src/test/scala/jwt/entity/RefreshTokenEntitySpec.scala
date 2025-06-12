package jwt.entity

import zio.*
import zio.test.*
import java.time.Instant
import java.util.UUID

object RefreshTokenEntitySpec extends ZIOSpecDefault:
  def spec =
    suite("RefreshTokenEntity")(
      test("RefreshTokenEntity should properly initialize and store values") {
        val now = Instant.now()
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val refreshToken = "jwt-token-string"
        val expiresAt = now.plusSeconds(86400) // +24 часа

        val entity = RefreshTokenEntity(id, userId, refreshToken, expiresAt, now)

        assertTrue(entity.id.equals(id)) &&
        assertTrue(entity.userId.equals(userId)) &&
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
            UUID.randomUUID(),
            UUID.randomUUID(),
            "token1",
            pastTime, // уже истек
            now.minusSeconds(7200), // создан 2 часа назад
          )

        val validEntity =
          RefreshTokenEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "token2",
            futureTime, // еще не истек
            now,
          )

        assertTrue(expiredEntity.expiresAt.isBefore(Instant.now())) &&
        assertTrue(validEntity.expiresAt.isAfter(Instant.now()))
      },
      test("Entities with same values should compare fields") {
        val now = Instant.now()
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val token = "same-token"

        val entity1 =
          RefreshTokenEntity(
            id,
            userId,
            token,
            now,
            now,
          )
        val entity2 =
          RefreshTokenEntity(
            id,
            userId,
            token,
            now,
            now,
          )

        assertTrue(entity1.id.equals(entity2.id)) &&
        assertTrue(entity1.userId.equals(entity2.userId)) &&
        assertTrue(entity1.refreshToken == entity2.refreshToken) &&
        assertTrue(entity1.expiresAt.equals(entity2.expiresAt)) &&
        assertTrue(entity1.createdAt.equals(entity2.createdAt))
      },
      test("Entities with different values should have different fields") {
        val now = Instant.now()
        val entity1 =
          RefreshTokenEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "token1",
            now,
            now,
          )
        val entity2 =
          RefreshTokenEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "token2",
            now,
            now,
          )

        assertTrue(!entity1.id.equals(entity2.id)) &&
        assertTrue(!entity1.userId.equals(entity2.userId)) &&
        assertTrue(!entity1.refreshToken.equals(entity2.refreshToken))
      },
    )
