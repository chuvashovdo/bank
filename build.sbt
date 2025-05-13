import sbt._

// Версии библиотек
lazy val versions =
  new {
    val zio =
      "2.0.19"
    val zioJson =
      "0.6.2"
    val zioHttp =
      "3.0.0-RC2"
    val tapir =
      "1.11.3"
    val openapiCirceYaml =
      "0.11.3"
    val quill =
      "4.8.0"
    val postgresql =
      "42.7.1"
    val flyway =
      "9.22.3"
    val logback =
      "1.4.14"
    val jwtScala =
      "9.4.5"
    val h2 =
      "2.2.224"
    val jbcrypt =
      "0.4"
    val scalacheck =
      "1.17.0"
    val munit =
      "0.7.29"
    val disciplineMunit =
      "2.0.0"
    val scalamock =
      "7.3.0"
    val iron =
      "3.0.1"
  }

ThisBuild / organization := "bank"
ThisBuild / scalaVersion := "3.6.2"
ThisBuild / version := "0.1.0-SNAPSHOT"

// Общие настройки SBT для всех пакетов
ThisBuild / scalacOptions ++= Seq(
  "-Ykind-projector",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:unsafeNulls",
)

ThisBuild / parallelExecution := true

// Зависимости по категориям
lazy val commonDependencies =
  Seq(
    "dev.zio" %% "zio" % versions.zio,
    "dev.zio" %% "zio-json" % versions.zioJson,
    "ch.qos.logback" % "logback-classic" % versions.logback,
  )

lazy val tapirDependencies =
  Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio" % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % versions.tapir,
    "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % versions.openapiCirceYaml,
  )

lazy val databaseDependencies =
  Seq(
    "io.getquill" %% "quill-jdbc-zio" % versions.quill,
    "org.postgresql" % "postgresql" % versions.postgresql,
    "org.flywaydb" % "flyway-core" % versions.flyway,
  )

lazy val commonTestDependencies =
  Seq(
    "org.scalacheck" %% "scalacheck" % versions.scalacheck,
    "org.scalameta" %% "munit-scalacheck" % versions.munit,
    "org.scalameta" %% "munit" % versions.munit,
    "org.typelevel" %% "discipline-munit" % versions.disciplineMunit,
  ).map(_ % Test)

lazy val h2TestDependency =
  Seq(
    "com.h2database" % "h2" % versions.h2 % Test
  )

// ZIO тестовые зависимости на уровне проекта
ThisBuild / libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test" % versions.zio % Test,
  "dev.zio" %% "zio-test-sbt" % versions.zio % Test,
  "dev.zio" %% "zio-test-magnolia" % versions.zio % Test,
  "org.scalamock" %% "scalamock" % versions.scalamock % Test,
)

// Настройка тестового фреймворка для sbt
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

// Общие тестовые настройки
lazy val commonTestSettings =
  Seq(
    Test / fork := true
  )

lazy val root =
  project
    .in(file("."))
    .aggregate(
      abstractions,
      authService,
      userService,
      jwtService,
      httpApi,
      app,
    )

lazy val abstractions =
  project
    .in(file("01-abstractions"))
    .settings(
      commonSettings,
      autoImportSettings,
      commonTestSettings,
      libraryDependencies ++= commonDependencies,
      libraryDependencies ++= commonTestDependencies,
      libraryDependencies += "io.github.iltotore" %% "iron" % versions.iron,
      Test / testOptions += Tests.Argument(TestFrameworks.MUnit, "+l"),
    )

lazy val serviceCommonSettings =
  commonSettings ++
    autoImportSettings ++
    commonTestSettings ++
    Seq(
      libraryDependencies ++= commonDependencies,
      libraryDependencies ++= databaseDependencies.filterNot(_.name == "flyway-core"),
      libraryDependencies ++= commonTestDependencies,
      libraryDependencies ++= h2TestDependency,
    )

lazy val authService =
  project
    .in(file("02-services/auth-service"))
    .dependsOn(abstractions)
    .settings(
      serviceCommonSettings
    )

lazy val userService =
  project
    .in(file("02-services/user-service"))
    .dependsOn(abstractions)
    .settings(
      serviceCommonSettings,
      libraryDependencies += "org.mindrot" % "jbcrypt" % versions.jbcrypt,
      libraryDependencies += "org.mindrot" % "jbcrypt" % versions.jbcrypt % Test,
    )

lazy val jwtService =
  project
    .in(file("02-services/jwt-service"))
    .dependsOn(abstractions)
    .settings(
      serviceCommonSettings,
      libraryDependencies ++= Seq(
        "com.github.jwt-scala" %% "jwt-core" % versions.jwtScala,
        "com.github.jwt-scala" %% "jwt-zio-json" % versions.jwtScala,
      ),
    )

lazy val httpApi =
  project
    .in(file("03-api"))
    .dependsOn(
      abstractions,
      authService,
      userService,
      jwtService,
    )
    .settings(
      commonSettings,
      autoImportSettings,
      commonTestSettings,
      libraryDependencies ++= commonDependencies,
      libraryDependencies ++= tapirDependencies,
      libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % versions.tapir,
      libraryDependencies += "dev.zio" %% "zio-http" % versions.zioHttp,
      libraryDependencies ++= commonTestDependencies,
    )

lazy val app =
  project
    .in(file("04-app"))
    .enablePlugins(JavaAppPackaging)
    .dependsOn(
      abstractions,
      httpApi,
    )
    .settings(
      commonSettings,
      autoImportSettings,
      commonTestSettings,
      libraryDependencies ++= commonDependencies,
      libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % versions.tapir,
      libraryDependencies += "dev.zio" %% "zio-http" % versions.zioHttp,
      libraryDependencies += "org.flywaydb" % "flyway-core" % versions.flyway,
      libraryDependencies ++= commonTestDependencies,
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
