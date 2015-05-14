//import play.PlayScala
//import NativePackagerKeys._

name := """tripspace"""

version := "1.0.0"

scalaVersion := "2.11.6"




bashScriptExtraDefines += """
function herokuUrlToJdbcUrl() {
	echo "$@" | sed -e 's#postgres://\(.*\):\(.*\)\@\(.*\)#jdbc:postgres://\3?user=\1\&password=\2#'
}

if [ "$DATABASE_URL" != "" ] ; then 
	JDBC_URL=$( herokuUrlToJdbcUrl "$DATABASE_URL" )

	addJava "-Dslick.dbs.default.db.url=${JDBC_URL}"

	addJava "-Dhttp.port=${PORT}"
fi	

"""


resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//resolvers += Resolver.sonatypeRepo("snapshots")

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  ws,
//  "com.mohiva" %% "play-silhouette" % "3.0.0-SNAPSHOT",
  "org.webjars" %% "webjars-play" % "2.4.0-RC1",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "jquery" % "2.1.4",
  "com.typesafe.akka" %% "akka-agent" % "2.3.9",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
//  "com.typesafe.play" %% "play-slick" % "1.0.0-SNAPSHOT",
// These should be removed after play-slick releases:
  "com.typesafe.play" %% "play-specs2" % "2.4.0-RC2",
  "com.typesafe.play" %% "play-jdbc" % "2.4.0-RC2",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
//  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0-SNAPSHOT" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.2",
  cache,
  "org.mindrot" % "jbcrypt" % "0.3m"
)

EclipseKeys.createSrc := EclipseCreateSrc.All

//CoffeeScriptKeys.sourceMap := true

lazy val root = (project in file(".")).enablePlugins(PlayScala,SbtWeb,JavaAppPackaging)


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


