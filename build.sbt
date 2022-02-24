lazy val sbtSassify = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(ScriptedPlugin)

name := "sbt-sassify"
organization := "io.github.irundaia"
organizationName := "Han van Venrooij"
startYear := Some(2022)
sbtPlugin := true
publishMavenStyle := true
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
homepage := Some(url("https://github.com/irundaia/sbt-sassify"))
scmInfo := Some(ScmInfo(
  url("https://github.com/irundaia/sbt-sassify"),
  "scm:git@github.com:irundaia/sbt-sassify.git"
))
developers := List(Developer(
  id="irundaia",
  name="Han van Venrooij",
  email="han.van.venrooij@icloud.com",
  url=url("https://github.com/irundaia"),
))

Test / fork := false

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.11" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.11" % "test",
  "org.scalatest" %% "scalatest-funspec" % "3.2.11" % "test",
  "net.java.dev.jna" % "jna" % "5.10.0",
  "io.spray" %%  "spray-json" % "1.3.6",
  "com.typesafe.akka" %% "akka-stream-typed" % "2.6.17",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
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

// Sonatype settings
import sbt.url
import xerial.sbt.Sonatype.GitHubHosting
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
publishTo := sonatypePublishToBundle.value
sonatypeProjectHosting := Some(GitHubHosting("irundaia", "sbt-sassify", "han.van.venrooij@icloud.com"))
sonatypeProfileName := "io.github.irundaia"
sonatypeDefaultResolver := Opts.resolver.sonatypeStaging
// Protobuf settings
Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

// Scalastyle settings
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := (Compile / scalastyle).toTask("").value
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
  releaseStepCommandAndRemaining("^publishSigned"),
  releaseStepCommandAndRemaining("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges)
