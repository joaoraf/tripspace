//import play.PlayScala
//import NativePackagerKeys._

name := """tripspace"""

version := "1.0.0"

scalaVersion := "2.11.6"


Revolver.settings

bashScriptExtraDefines += """
function addHerokuParams() {
	DBVARS=`echo "$DATABASE_URL" | sed -e 's#postgres://\(.*\):\(.*\)\@\(.*\)#DB_HOST=\3 DB_USER=\1 DB_PASSWORD=\2#'`
	echo "DBVARS=$DBVARS"
	if [ "$DBVARS" == "" ] ; then
		echo "Not export DBVARS"
		exit 1
	else
		echo "Exporting"
		export $DBVARS
		echo "Env:"
		set
	fi
	addJava "-Dslick.dbs.default.db.url=jdbc:postgresql://$DB_HOST"
	addJava "-Dslick.dbs.default.db.user=$DB_USER"
	addJava "-Dslick.dbs.default.db.password=$DB_PASSWORD"
	addJava "-Dhttp.port=${PORT}"
}

if [ "$DATABASE_URL" != "" ] ; then 
	addHerokuParams
fi	

"""


resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//resolvers += Resolver.sonatypeRepo("snapshots")

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  ws,
//  "com.mohiva" %% "play-silhouette" % "3.0.0-SNAPSHOT",
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "angular-ui-bootstrap" % "0.13.0",
  "org.webjars" % "angularjs" % "1.4.2",
  "org.webjars" % "selectize.js" % "0.12.1",
  "com.typesafe.akka" %% "akka-agent" % "2.3.9",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
//  "com.typesafe.play" %% "play-slick" % "1.0.0-SNAPSHOT",
// These should be removed after play-slick releases:
  "com.typesafe.play" %% "play-specs2" % "2.4.0",
  "com.typesafe.play" %% "play-jdbc" % "2.4.0",
  "com.typesafe.slick" %% "slick" % "3.0.0",  
  "mysql" % "mysql-connector-java" % "5.1.32",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
//  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0-SNAPSHOT" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.2",
  cache,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.zaxxer" % "HikariCP" % "2.3.7",
  "org.apache.jena" % "apache-jena-libs" % "2.13.0"
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
  "-Xlint:-missing-interpolator",
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


