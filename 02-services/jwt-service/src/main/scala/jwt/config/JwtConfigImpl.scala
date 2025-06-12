package jwt.config

import zio.*

object JwtConfigImpl:
  val layer: Layer[Config.Error, JwtConfig] =
    ZLayer.fromZIO:
      ZIO.config(JwtConfigDescriptor.descriptor)
