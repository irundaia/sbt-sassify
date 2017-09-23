// Project plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")

// Testing plugins
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

// Style and code style plugins

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "3.0.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
