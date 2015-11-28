import bintray.BintrayPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.heikoseeberger.sbtheader.HeaderKey._
import sbt.Keys._
import sbt._

object SassifyBuild extends Build {

  val directoryStructureSettings = Seq(
    scalaSource in Compile := baseDirectory.value / "src",
    javaSource in Compile := baseDirectory.value / "src",
    sourceDirectories in Compile := Seq((scalaSource in Compile).value),
    resourceDirectory in Compile := baseDirectory.value / "resources",
    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Test := baseDirectory.value / "test",
    sourceDirectories in Test := Seq((scalaSource in Test).value),
    resourceDirectory in Test := baseDirectory.value / "test-resources"
  )

  val compilerSettings = Seq (
    scalaVersion := "2.10.6",
    sourcesInBase := false,
    crossPaths := false,
    scalacOptions ++= Seq(
      "-unchecked",
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings",
      "-feature",
      "-encoding",
      "UTF-8"
    )
  )

  val bintraySettings = Seq(
    bintrayOrganization in bintray := None,
    bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sbt-sassify"),
    bintrayRepository := "sbt-plugins",
    bintrayReleaseOnPublish in ThisBuild := false,
    publishMavenStyle := false,
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publish <<= publish dependsOn (test in Test)
  )

  val copyrightSettings = Seq(
    headers := Map(
      "scala" -> Apache2_0("2015", "Han van Venrooij")
    )
  )

  val sbtWebSettings = Seq(
    resolvers ++= List(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.2")
  )

  val sbtGitSettings = Seq(
    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
  )

  // File copyright headers
  lazy val sbtSassify = project
    .in(file("."))
    .settings(directoryStructureSettings: _*)
    .settings(compilerSettings: _*)
    .settings(bintraySettings: _*)
    .enablePlugins(AutomateHeaderPlugin)
    .settings(copyrightSettings: _*)
    .settings(sbtWebSettings: _*)
    .enablePlugins(GitVersioning)
}