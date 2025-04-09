package jwt.repository

import jwt.entity.RefreshTokenEntity
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import jwt.models.RefreshToken
import jwt.service.JwtService
import user.models.UserId
import java.util.UUID

class TokenRepositoryImpl(quill: Quill.Postgres[SnakeCase], jwtService: JwtService)
    extends TokenRepository:
  import quill.*

  inline given tokenSchemaMeta: SchemaMeta[RefreshTokenEntity] =
    schemaMeta("refresh_tokens")

  override def saveRefreshToken(refreshToken: RefreshToken): Task[Unit] =
    for
      tokenEntity <-
        jwtService.createRefreshTokenEntity(
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

  override def findByRefreshToken(token: String): Task[Option[RefreshToken]] =
    run {
      quote:
        query[RefreshTokenEntity].filter { t =>
          t.refreshToken == lift(token) &&
          infix"${t.expiresAt} > ${lift(java.time.Instant.now())}".as[Boolean]
        }
    }.map:
      _.headOption.map(entity =>
        RefreshToken(
          token = entity.refreshToken,
          expiresAt = entity.expiresAt,
          userId = UserId(entity.userId),
        )
      )

  override def deleteByRefreshToken(token: String): Task[Unit] =
    run(quote {
      query[RefreshTokenEntity].filter(_.refreshToken == lift(token)).delete
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
  val layer: URLayer[Quill.Postgres[SnakeCase] & JwtService, TokenRepository] =
    ZLayer:
      for
        quill <- ZIO.service[Quill.Postgres[SnakeCase]]
        jwtService <- ZIO.service[JwtService]
        impl <- ZIO.succeed(TokenRepositoryImpl(quill, jwtService))
      yield impl
