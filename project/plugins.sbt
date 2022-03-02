// Project plugins
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

// Testing plugins
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

// Style and code style plugins

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "3.0.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.3")

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
