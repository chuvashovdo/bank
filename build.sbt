ThisBuild / organization := "dev.insideyou"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root =
  (project in file("."))
    .aggregate(
      abstractions,
      userService,
      accountService,
      app,
    )
    .settings(commonSettings)
    .settings(autoImportSettings)

lazy val abstractions =
  (project in file("01-abstractions"))
    .settings(
      name := "abstractions",
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
      ),
      libraryDependencies ++= Seq(
        "com.eed3si9n" %% "expecty" % "0.16.0",
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
    )

lazy val userService =
  (project in file("02-services/user-service"))
    .dependsOn(abstractions)
    .settings(
      name := "user-service",
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
        "org.postgresql" % "postgresql" % "42.7.1",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
      ),
      libraryDependencies ++= Seq(
        "com.eed3si9n" %% "expecty" % "0.16.0",
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
    )

lazy val accountService =
  (project in file("02-services/account-service"))
    .dependsOn(abstractions)
    .settings(
      name := "account-service",
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
        "org.postgresql" % "postgresql" % "42.7.1",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
      ),
      libraryDependencies ++= Seq(
        "com.eed3si9n" %% "expecty" % "0.16.0",
        "org.scalacheck" %% "scalacheck" % "1.17.0",
        "org.scalameta" %% "munit-scalacheck" % "0.7.29",
        "org.scalameta" %% "munit" % "0.7.29",
        "org.typelevel" %% "discipline-munit" % "2.0.0",
      ).map(_ % Test),
    )

lazy val app =
  (project in file("03-app"))
    .dependsOn(abstractions, userService, accountService)
    .settings(
      name := "app",
      commonSettings,
      autoImportSettings,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "2.0.19",
        "dev.zio" %% "zio-json" % "0.6.2",
        "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.9",
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.9",
        "ch.qos.logback" % "logback-classic" % "1.4.14",
      ),
      libraryDependencies ++= Seq(
        "com.eed3si9n" %% "expecty" % "0.16.0",
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
