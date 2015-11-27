name := "sbt-sassify"
organization := "org.irundaia.sbt"
version := "1.1.0"
sbtPlugin := true
scalaVersion := "2.10.6"
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

// Library dependencies
resolvers ++= List(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.2")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.4",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "io.bit3" % "jsass" % "3.3.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)

// Publishing settings
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false
publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publish <<= publish dependsOn (test in Test)
