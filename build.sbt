import Dependencies._

// ThisBuild / organization := "dev.insideyou"
ThisBuild / scalaVersion := "3.3.1"

lazy val `bank` =
  project
    .in(file("."))
    .aggregate(
      core,
      main,
    )

// lazy val `bank` =
//   project
//     .in(file("."))
//     .settings(name := "bank")
//     .settings(commonSettings)
//     .settings(autoImportSettings)
//     .settings(dependencies)

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
      // cs launch scalac:3.3.1 -- -Wconf:help
      // src is not yet available for Scala3
      // scalacOptions += s"-Wconf:src=${target.value}/.*:s",
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

lazy val dependencies =
  Seq(
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
      com.eed3si9n.expecty.expecty,
      org.scalacheck.scalacheck,
      org.scalameta.`munit-scalacheck`,
      org.scalameta.munit,
      org.typelevel.`discipline-munit`,
    ).map(_ % Test),
  )
