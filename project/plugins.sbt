// Project plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

// Testing plugins
libraryDependencies <+= sbtVersion(v => "org.scala-sbt" % "scripted-plugin" % v)

// Style and code style plugins

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.12")