val ZioVersion           = "1.0.0-RC11-1"
val ZioCatsVersion       = "2.0.0.0-RC2"
val CatsVersion          = "2.0.0-RC1"
val LogbackVersion       = "1.2.3"
val ScalaLogVersion      = "3.9.2"
val ConfigFactoryVersion = "1.3.4"
val ScalaTestVersion     = "3.0.8"
val ScalaCheckVersion    = "1.14.0"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    organization := "CloverGroup",
    name := "zio-core",
    version := "0.0.1",
    scalaVersion := "2.12.8",
    maxErrors := 3,
    libraryDependencies ++= Seq(
      "org.scalactic"              %% "scalactic"        % ScalaTestVersion,
      "org.scalatest"              %% "scalatest"        % ScalaTestVersion % "test",
      "org.scalacheck"             %% "scalacheck"       % ScalaCheckVersion % "test",
      "dev.zio"                    %% "zio"              % ZioVersion,
      "dev.zio"                    %% "zio-interop-cats" % ZioCatsVersion,
      "org.typelevel"              %% "cats-core"        % CatsVersion,
      "ch.qos.logback"             % "logback-classic"   % LogbackVersion,
      "com.typesafe.scala-logging" %% "scala-logging"    % ScalaLogVersion,
      "com.typesafe"               % "config"            % ConfigFactoryVersion
    )
  )

scalacOptions --= Seq(
  "-Xfatal-warnings"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("chk", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
