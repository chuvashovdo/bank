package jwt.config

import zio.Config

object JwtConfigDescriptor:
  val descriptor: Config[JwtConfig] =
    (
      Config.string("secretKey") ++
        Config.duration("accessTokenExpiration") ++
        Config.duration("refreshTokenExpiration") ++
        Config.string("issuer") ++
        Config.string("audience")
    ).map {
      case (secretKey, accessTokenExpiration, refreshTokenExpiration, issuer, audience) =>
        JwtConfig(secretKey, accessTokenExpiration, refreshTokenExpiration, issuer, audience)
    }.nested("jwt")
