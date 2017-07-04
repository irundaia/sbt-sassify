// Project plugins
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

// Testing plugins
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

// Style and code style plugins

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "3.0.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
