name := "sbt-sassify"
organization := "org.irundaia.sbt"
version := "0.9.2"
sbtPlugin := true
scalaVersion := "2.10.5"
sourcesInBase := false
crossPaths := false
scalacOptions ++= Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-Xfatal-warnings",
  "-feature",
  "-encoding",
  "UTF-8"
)


incOptions := incOptions.value.withNameHashing(nameHashing = true)

// Compile settings
scalaSource in Compile := baseDirectory.value / "src"

javaSource in Compile := baseDirectory.value / "src"

sourceDirectories in Compile := Seq((scalaSource in Compile).value)

resourceDirectory in Compile := baseDirectory.value / "resources"

// Test settings
scalaSource in Test := baseDirectory.value / "test"

javaSource in Test := baseDirectory.value / "test"

sourceDirectories in Test := Seq((scalaSource in Test).value)

resourceDirectory in Test := baseDirectory.value / "test-resources"

resolvers ++= List(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.2")

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.3"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"

libraryDependencies += "io.bit3" % "jsass" % "3.3.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

addCommandAlias("pub", ";reload;publish;bintrayRelease")


// Publishing options
// ==================
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify")
bintrayReleaseOnPublish in ThisBuild := false
bintrayRepository := "sbt-plugins"
publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publish <<= publish dependsOn (test in Test)
