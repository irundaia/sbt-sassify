import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.git

import java.time.LocalDate

lazy val sbtSassify = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(GitVersioning)
  .enablePlugins(ScriptedPlugin)
  .settings(git.gitUncommittedChanges := false)

name := "sbt-sassify"
organization := "org.irundaia.sbt"
organizationName := "Han van Venrooij"
startYear := Some(2017)
sbtPlugin := true

fork in Test := true

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.2")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "net.java.dev.jna" % "jna" % "4.2.2"
)

// Compiler settings
crossSbtVersions := Seq("1.0.1", "0.13.16")
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

// Bintray settings
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false
publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publish := (publish dependsOn (test in Test)).value

// Scalastyle settings
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := scalastyle.in(Compile).toTask("").value
scalastyleFailOnError := true

// Scripted settings
scriptedBufferLog := false
scriptedLaunchOpts += "-Dplugin.version=" + version.value
