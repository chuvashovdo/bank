import sbt._

ThisBuild / organization := "bank"
ThisBuild / scalaVersion := "3.6.4"
ThisBuild / version := "0.1.0-SNAPSHOT"

// Общие настройки SBT для всех пакетов
ThisBuild / scalacOptions ++= Seq(
  "-Ykind-projector",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:unsafeNulls",
)

// Общие зависимости для тестов, добавляются к ThisBuild
ThisBuild / libraryDependencies ++= Seq(
  // ZIO Test - основной фреймворк для тестирования
  "dev.zio" %% "zio-test" % "2.0.19" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.0.19" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.0.19" % Test,

  // ScalaMock - для создания моков (если нужны)
  "org.scalamock" %% "scalamock" % "7.3.0" % Test,
)

// Настройка тестового фреймворка для sbt
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root =
  project
    .in(file("."))
    .aggregate(
      abstractions,
      authService,
      userService,
      jwtService,
      main,
    )

lazy val abstractions =
  project
    .in(file("01-abstractions"))
    .settings(
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
      ),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
      Test / fork := true,
      Test / testOptions += Tests.Argument(TestFrameworks.MUnit, "+l"),
    )

lazy val authService =
  project
    .in(file("02-services/auth-service"))
    .dependsOn(abstractions)
    .settings(
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.9",
        "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.7.3",
        "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
        "org.postgresql" % "postgresql" % "42.7.1",
        "org.flywaydb" % "flyway-core" % "9.22.3",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
      ),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
      Test / fork := true,
    )

lazy val userService =
  project
    .in(file("02-services/user-service"))
    .dependsOn(abstractions)
    .settings(
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.9",
        "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.7.3",
        "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
        "org.postgresql" % "postgresql" % "42.7.1",
        "org.flywaydb" % "flyway-core" % "9.22.3",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
        "org.mindrot" % "jbcrypt" % "0.4",
      ),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
        "com.h2database" % "h2" % "2.2.224",
      ).map(_ % Test),
      Test / fork := true,
    )

lazy val jwtService =
  project
    .in(file("02-services/jwt-service"))
    .dependsOn(abstractions)
    .settings(
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.9",
        "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.7.3",
        "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
        "org.postgresql" % "postgresql" % "42.7.1",
        "org.flywaydb" % "flyway-core" % "9.22.3",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
        "com.github.jwt-scala" %% "jwt-core" % "9.4.5",
        "com.github.jwt-scala" %% "jwt-zio-json" % "9.4.5",
      ),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
        "com.h2database" % "h2" % "2.2.224",
      ).map(_ % Test),
      Test / fork := true,
    )

import com.typesafe.sbt.packager.docker._

lazy val main =
  project
    .in(file("03-app"))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .dependsOn(
      abstractions,
      authService,
      userService,
      jwtService,
    )
    .settings(
      commonSettings,
      autoImportSettings,
      // Docker настройки
      Docker / packageName := "bank-app",
      Docker / version := "latest",
      Docker / maintainer := "dev@example.com",
      dockerBaseImage := "openjdk:17-jdk-slim",
      dockerExposedPorts ++= Seq(8080),
      dockerEnvVars := Map(
        "DB_HOST" -> "postgres",
        "DB_PORT" -> "5432",
        "DB_NAME" -> "bank",
        "DB_USER" -> "postgres",
        "DB_PASSWORD" -> "postgres",
      ),
      dockerUpdateLatest := true,
      // Настройки для запуска
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "dev.zio" %% "zio-http" % "3.0.0-RC2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.9",
        "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.7.3",
        "org.flywaydb" % "flyway-core" % "9.22.3",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
      ),
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
    )

lazy val commonSettings = {
  lazy val commonScalacOptions =
    Seq(
      Compile / console / scalacOptions := {
        (Compile / console / scalacOptions)
          .value
          .filterNot(_.contains("wartremover"))
          .filterNot(Scalac.Lint.toSet)
          .filterNot(Scalac.FatalWarnings.toSet) :+ "-Wconf:any:silent"
      },
      Test / console / scalacOptions :=
        (Compile / console / scalacOptions).value,
    )

  lazy val otherCommonSettings =
    Seq(
      update / evictionWarningOptions := EvictionWarningOptions.empty
    )

  Seq(
    commonScalacOptions,
    otherCommonSettings,
  ).reduceLeft(_ ++ _)
}

lazy val autoImportSettings =
  Seq(
    scalacOptions +=
      Seq(
        "java.lang",
        "scala",
        "scala.Predef",
        "scala.annotation",
        "scala.util.chaining",
      ).mkString(start = "-Yimports:", sep = ",", end = ""),
    Test / scalacOptions +=
      Seq(
        "org.scalacheck",
        "org.scalacheck.Prop",
      ).mkString(start = "-Yimports:", sep = ",", end = ""),
  )
