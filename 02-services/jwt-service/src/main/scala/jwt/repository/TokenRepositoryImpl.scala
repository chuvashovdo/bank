package jwt.repository

import jwt.entity.RefreshTokenEntity
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import jwt.models.{ RefreshToken, JwtRefreshToken }
import user.models.UserId
import auth.errors.RefreshTokenNotFoundError
import common.errors.CorruptedDataInDBError
import java.util.UUID

class TokenRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends TokenRepository:
  import quill.*

  inline given tokenSchemaMeta: SchemaMeta[RefreshTokenEntity] =
    schemaMeta[RefreshTokenEntity]("refresh_tokens")

  override def saveRefreshToken(refreshTokenEntity: RefreshTokenEntity): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].insertValue(lift(refreshTokenEntity))
    }).unit

  override def findByRefreshToken(token: String): Task[RefreshToken] =
    run {
      quote:
        query[RefreshTokenEntity].filter { t =>
          t.refreshToken == lift(token) &&
          infix"${t.expiresAt} > ${lift(java.time.Instant.now())}".as[Boolean]
        }
    }.map(_.headOption)
      .flatMap:
        case Some(entity: RefreshTokenEntity) =>
          val validatedTokenZIO: Task[JwtRefreshToken] =
            ZIO
              .fromEither(JwtRefreshToken(entity.refreshToken))
              .mapError { validationError =>
                CorruptedDataInDBError(
                  entityId = entity.id.toString,
                  fieldName = "refreshToken",
                  fieldValue = entity.refreshToken,
                  validationErrorMessage = validationError.developerFriendlyMessage,
                )
              }

          for
            validToken <- validatedTokenZIO
            validUserId = UserId(entity.userId)
          yield RefreshToken(token = validToken, expiresAt = entity.expiresAt, userId = validUserId)

        case None =>
          ZIO.fail(RefreshTokenNotFoundError(token))

  override def deleteByRefreshToken(token: String): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].filter(_.refreshToken == lift(token)).delete
    }).unit

  override def deleteAllByUserId(userId: UUID): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].filter(_.userId.equals(lift(userId))).delete
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
