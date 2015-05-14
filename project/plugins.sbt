// Comment to get more information during initialization
// logLevel := Level.Warn

// The Sonatype snapshots repository
resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.typesafeRepo("releases")

resolvers += Resolver.typesafeRepo("snapshots")

resolvers += Resolver.sbtPluginRepo("releases")

resolvers += Resolver.sbtPluginRepo("snapshots")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-RC1")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0-RC2")

// Use the Scalariform plugin to reformat the code
//addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// web plugins

//addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")

//addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")

//addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")


