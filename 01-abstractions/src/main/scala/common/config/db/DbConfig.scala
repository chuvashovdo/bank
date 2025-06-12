package common.config.db

final case class DbConfig(
  host: String,
  port: Int,
  database: String,
  user: String,
  password: String,
)
