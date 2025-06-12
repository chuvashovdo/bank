package jwt.config

import zio.*
import zio.Duration

final case class JwtConfig(
  secretKey: String,
  accessTokenExpiration: Duration,
  refreshTokenExpiration: Duration,
  issuer: String,
  audience: String,
)
