package common.config.db

import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.*
import scala.language.unsafeNulls
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import javax.sql.DataSource

object DbConfigImpl:
  val layer: Layer[Config.Error, DbConfig] =
    ZLayer.fromZIO:
      ZIO.config(DbConfigDescriptor.descriptor)

object QuillContext:
  val layer: ZLayer[DbConfig, Throwable, Quill.Postgres[SnakeCase.type]] =
    ZLayer:
      for
        config <- ZIO.service[DbConfig]
        dataSource <-
          ZIO.succeed:
            val hikariConfig = new HikariConfig()
            hikariConfig.setJdbcUrl(
              s"jdbc:postgresql://${config.host}:${config.port}/${config.database}"
            )
            hikariConfig.setUsername(config.user)
            hikariConfig.setPassword(config.password)
            hikariConfig.setDriverClassName("org.postgresql.Driver")
            hikariConfig.setMaximumPoolSize(10)
            new HikariDataSource(hikariConfig)
      yield new Quill.Postgres(SnakeCase, dataSource.asInstanceOf[DataSource])
