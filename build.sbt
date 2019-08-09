val ZioVersion      = "1.0.0-RC11-1"
val ZioCatsVersion  = "2.0.0.0-RC2"
val CatsVersion     = "2.0.0-RC1"
val LogbackVersion  = "1.2.3"
val ScalaLogVersion = "3.9.2"
val Specs2Version   = "4.7.0"
val ParadiseVersion = "2.1.1"

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
      "org.specs2"                 %% "specs2-core"      % Specs2Version % Test,
      "dev.zio"                    %% "zio"              % ZioVersion,
      "dev.zio"                    %% "zio-interop-cats" % ZioCatsVersion,
      "org.typelevel"              %% "cats-core"        % CatsVersion,
      "ch.qos.logback"             % "logback-classic"   % LogbackVersion,
      "com.typesafe.scala-logging" %% "scala-logging"    % ScalaLogVersion
    )
  )

scalacOptions --= Seq(
  "-Xfatal-warnings"
)

addCompilerPlugin("org.scalamacros" % "paradise" % ParadiseVersion cross CrossVersion.full)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("chk", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
