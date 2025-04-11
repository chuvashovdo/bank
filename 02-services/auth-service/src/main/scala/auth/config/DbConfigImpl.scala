package auth.config

import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.*
import scala.language.unsafeNulls
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import javax.sql.DataSource

object DbConfigImpl:
  val layer: ULayer[DbConfig] =
    ZLayer.succeed(
      DbConfig(
        url = "jdbc:postgresql://localhost:5432/bank",
        user = "postgres",
        password = "postgres",
      )
    )

object QuillContext:
  val dataSourceLayer: URLayer[DbConfig, Quill.Postgres[SnakeCase.type]] =
    ZLayer:
      for
        config <- ZIO.service[DbConfig]
        dataSource <-
          ZIO.succeed:
            val hikariConfig = new HikariConfig()
            hikariConfig.setJdbcUrl(config.url)
            hikariConfig.setUsername(config.user)
            hikariConfig.setPassword(config.password)
            hikariConfig.setDriverClassName("org.postgresql.Driver")
            hikariConfig.setMaximumPoolSize(10)
            new HikariDataSource(hikariConfig)
      // Создаем Quill.Postgres явно
      yield new Quill.Postgres(SnakeCase, dataSource.asInstanceOf[DataSource])
