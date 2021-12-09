lazy val sbtSassify = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(ScriptedPlugin)

name := "sbt-sassify"
organization := "org.irundaia.sbt"
organizationName := "Han van Venrooij"
startYear := Some(2018)
sbtPlugin := true

fork in Test := false

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.10" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.10" % "test",
  "org.scalatest" %% "scalatest-funspec" % "3.2.10" % "test",
  "net.java.dev.jna" % "jna" % "5.10.0"
)

// Compiler settings
crossSbtVersions := Seq("1.1.1", "0.13.16")
sourcesInBase := false
crossPaths := false
scalacOptions ++= Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-feature",
  "-encoding",
  "UTF-8"
)
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

// Bintray settings
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false
publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

// Scalastyle settings
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := scalastyle.in(Compile).toTask("").value
scalastyleFailOnError := true

// Scripted settings
scriptedBufferLog := false
scriptedLaunchOpts += "-Dplugin.version=" + version.value

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("test"),
  releaseStepCommandAndRemaining("scripted"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepCommandAndRemaining("^ publish"),
  releaseStepCommandAndRemaining("bintrayRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges)
