import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.SbtGit.git
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.heikoseeberger.sbtheader.HeaderKey._
import java.time.LocalDate

lazy val sbtSassify = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(GitVersioning)
  .settings(ScriptedPlugin.scriptedSettings)
  .settings(git.gitUncommittedChanges := false)

name := "sbt-sassify"
organization := "org.irundaia.sbt"
sbtPlugin := true

fork in Test := true

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.2")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "net.java.dev.jna" % "jna" % "4.2.2"
)

// Compiler settings
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

// Bintray settings
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false
publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publish := (publish dependsOn (test in Test)).value

headers := Map(
  "scala" -> Apache2_0(LocalDate.now().getYear.toString, "Han van Venrooij"),
  "java" -> Apache2_0(LocalDate.now.getYear.toString, "Han van Venrooij")
)

// Scalastyle settings
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value
org.scalastyle.sbt.ScalastylePlugin.scalastyleFailOnError := true

// Scripted settings
ScriptedPlugin.scriptedBufferLog := false
ScriptedPlugin.scriptedLaunchOpts += "-Dplugin.version=" + version.value
