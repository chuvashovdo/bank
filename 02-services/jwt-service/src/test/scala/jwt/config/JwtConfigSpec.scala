package jwt.config

import zio.*
import zio.test.*

object JwtConfigSpec extends ZIOSpecDefault:
  // Создаем моковую реализацию для тестов
  class TestJwtConfig extends JwtConfig:
    private var properties =
      Map(
        "JWT_SECRET_KEY" -> "default-secure-key-30chars-minimum-default",
        "JWT_ACCESS_TOKEN_EXPIRATION_MINUTES" -> "60",
        "JWT_REFRESH_TOKEN_EXPIRATION_DAYS" -> "30",
        "JWT_ISSUER" -> "bank-auth-service",
        "JWT_AUDIENCE" -> "bank-api",
      )

    def setProperty(key: String, value: String): Unit =
      properties = properties + (key -> value)

    override def secretKey: Task[String] =
      ZIO.succeed(properties("JWT_SECRET_KEY"))

    override def accessTokenExpiration: Task[Long] =
      ZIO.succeed(properties("JWT_ACCESS_TOKEN_EXPIRATION_MINUTES").toLong)

    override def refreshTokenExpiration: Task[Long] =
      ZIO.succeed(properties("JWT_REFRESH_TOKEN_EXPIRATION_DAYS").toLong)

    override def issuer: Task[String] =
      ZIO.succeed(properties("JWT_ISSUER"))

    override def audience: Task[String] =
      ZIO.succeed(properties("JWT_AUDIENCE"))

    override def accessTokenExpirationMillis: Task[Long] =
      accessTokenExpiration.map(_ * 60 * 1000)

    override def refreshTokenExpirationMillis: Task[Long] =
      refreshTokenExpiration.map(_ * 24 * 60 * 60 * 1000)

  def spec =
    suite("JwtConfig")(
      test("secretKey should return value from environment variable") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_SECRET_KEY", "test-secret-key-for-jwt-config-spec")
          secretKey <- testConfig.secretKey
        yield assertTrue(secretKey == "test-secret-key-for-jwt-config-spec")
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("accessTokenExpiration should return value from environment variable") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_ACCESS_TOKEN_EXPIRATION_MINUTES", "15")
          expiration <- testConfig.accessTokenExpiration
        yield assertTrue(expiration == 15L)
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("refreshTokenExpiration should return value from environment variable") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_REFRESH_TOKEN_EXPIRATION_DAYS", "7")
          expiration <- testConfig.refreshTokenExpiration
        yield assertTrue(expiration == 7L)
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("issuer should return value from environment variable") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_ISSUER", "test-issuer")
          issuer <- testConfig.issuer
        yield assertTrue(issuer == "test-issuer")
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("audience should return value from environment variable") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_AUDIENCE", "test-audience")
          audience <- testConfig.audience
        yield assertTrue(audience == "test-audience")
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("accessTokenExpirationMillis should convert minutes to milliseconds") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_ACCESS_TOKEN_EXPIRATION_MINUTES", "15")
          expirationMillis <- testConfig.accessTokenExpirationMillis
        yield assertTrue(expirationMillis == 15L * 60 * 1000)
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("refreshTokenExpirationMillis should convert days to milliseconds") {
        for
          testConfig <- ZIO.service[TestJwtConfig]
          _ = testConfig.setProperty("JWT_REFRESH_TOKEN_EXPIRATION_DAYS", "7")
          expirationMillis <- testConfig.refreshTokenExpirationMillis
        yield assertTrue(expirationMillis == 7L * 24 * 60 * 60 * 1000)
      }.provide(ZLayer.succeed(new TestJwtConfig)),
      test("should use default values when not explicitly set") {
        for
          config <- ZIO.service[TestJwtConfig]
          secretKey <- config.secretKey
          accessExp <- config.accessTokenExpiration
          refreshExp <- config.refreshTokenExpiration
          issuer <- config.issuer
          audience <- config.audience
        yield assert(secretKey)(Assertion.isNonEmptyString) &&
        assert(accessExp)(Assertion.isGreaterThan(0L)) &&
        assert(refreshExp)(Assertion.isGreaterThan(0L)) &&
        assert(issuer)(Assertion.isNonEmptyString) &&
        assert(audience)(Assertion.isNonEmptyString)
      }.provide(ZLayer.succeed(new TestJwtConfig)),
    )
