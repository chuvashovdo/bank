package jwt.repository

import jwt.entity.RefreshTokenEntity
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import jwt.models.{ RefreshToken, JwtRefreshToken }
import user.models.UserId
import java.time.Instant
import java.util.UUID
import common.errors.ValidationError

class TokenRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends TokenRepository:
  import quill.*

  inline given tokenSchemaMeta: SchemaMeta[RefreshTokenEntity] =
    schemaMeta[RefreshTokenEntity]("refresh_tokens")

  private def createRefreshTokenEntity(
    id: String,
    userId: UserId,
    refreshToken: JwtRefreshToken,
    expiresAt: Instant,
  ): Task[RefreshTokenEntity] =
    ZIO.succeed(RefreshTokenEntity(id, userId.value, refreshToken.value, expiresAt, Instant.now()))

  override def saveRefreshToken(refreshToken: RefreshToken): Task[Unit] =
    for
      tokenEntity <-
        createRefreshTokenEntity(
          id = UUID.randomUUID().toString,
          userId = refreshToken.userId,
          refreshToken = refreshToken.token,
          expiresAt = refreshToken.expiresAt,
        )
      _ <-
        run(quote {
          query[RefreshTokenEntity].insertValue(lift(tokenEntity))
        }).unit
    yield ()

  override def findByRefreshToken(token: JwtRefreshToken): Task[Option[RefreshToken]] =
    run {
      quote:
        query[RefreshTokenEntity].filter { t =>
          t.refreshToken == lift(token.value) &&
          infix"${t.expiresAt} > ${lift(java.time.Instant.now())}".as[Boolean]
        }
    }.map(_.headOption)
      .flatMap:
        case Some(entity: RefreshTokenEntity) =>
          val refreshTokenEither: Either[ValidationError, JwtRefreshToken] =
            JwtRefreshToken(entity.refreshToken)
          val validatedTokenZIO: Task[JwtRefreshToken] =
            ZIO
              .fromEither(refreshTokenEither)
              .mapError { err =>
                new IllegalArgumentException(
                  s"Invalid refresh token format in DB (id: ${entity.id}): ${err.developerFriendlyMessage}"
                )
              }

          val userIdEither: Either[ValidationError, UserId] = UserId(entity.userId)
          val validatedUserIdZIO: Task[UserId] =
            ZIO
              .fromEither(userIdEither)
              .mapError { err =>
                new IllegalArgumentException(
                  s"Invalid user ID format in DB (id: ${entity.id}): ${err.developerFriendlyMessage}"
                )
              }

          for
            validToken <- validatedTokenZIO
            validUserId <- validatedUserIdZIO
          yield Some(
            RefreshToken(token = validToken, expiresAt = entity.expiresAt, userId = validUserId)
          )
        case None => ZIO.succeed(None)

  override def deleteByRefreshToken(token: JwtRefreshToken): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].filter(_.refreshToken == lift(token.value)).delete
    }).unit

  override def deleteAllByUserId(userId: UserId): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].filter(_.userId == lift(userId.value)).delete
    }).unit

  override def cleanExpiredTokens(): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity]
        .filter(t => infix"${t.expiresAt} < ${lift(java.time.Instant.now())}".as[Boolean])
        .delete
    }).unit

object TokenRepositoryImpl:
  val layer: URLayer[Quill.Postgres[SnakeCase], TokenRepository] =
    ZLayer:
      for
        quill <- ZIO.service[Quill.Postgres[SnakeCase]]
        impl <- ZIO.succeed(TokenRepositoryImpl(quill))
      yield impl
