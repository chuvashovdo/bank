package common.config.db

import zio.Config
import zio.config.magnolia.deriveConfig

object DbConfigDescriptor:
  val descriptor: Config[DbConfig] =
    deriveConfig[DbConfig].nested("db")
