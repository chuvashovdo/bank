package common.db

import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.*
import scala.language.unsafeNulls
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import javax.sql.DataSource

object DbConfigImpl:
  val layer: ULayer[DbConfig] =
    ZLayer.succeed:
      val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
      val dbPort = sys.env.getOrElse("DB_PORT", "5432")
      val dbName = sys.env.getOrElse("DB_NAME", "bank")
      val dbUser = sys.env.getOrElse("DB_USER", "postgres")
      val dbPassword = sys.env.getOrElse("DB_PASSWORD", "postgres")

      DbConfig(
        url = s"jdbc:postgresql://$dbHost:$dbPort/$dbName",
        user = dbUser,
        password = dbPassword,
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
      yield new Quill.Postgres(SnakeCase, dataSource.asInstanceOf[DataSource])
