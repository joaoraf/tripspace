import play.PlayScala

name := """tripspace"""

version := "1.0.0"

scalaVersion := "2.11.6"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += Resolver.sonatypeRepo("snapshots")



libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "2.1-SNAPSHOT",
  "org.webjars" %% "webjars-play" % "2.3.0-3",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "jquery" % "2.1.4",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.mohiva" %% "play-silhouette-testkit" % "2.1-SNAPSHOT" % "test",
  cache
)

EclipseKeys.createSrc := EclipseCreateSrc.All

CoffeeScriptKeys.sourceMap := true

lazy val root = (project in file(".")).enablePlugins(PlayScala,SbtWeb)


scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

//********************************************************
// Scalariform settings
//********************************************************

/*
defaultScalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)
*/  
