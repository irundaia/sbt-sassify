import scala.io.Codec
import org.irundaia.sbt.sass._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

lazy val playVersion = settingKey[String]("Play version in relation to current Scala Version")

playVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => "2.4.0-2"
    case Some((2, n)) if n > 10 => "2.6.2"
  }
}

SassKeys.floatingPointPrecision := 5

libraryDependencies ++= Seq(
  "org.webjars.npm" % "fortawesome__fontawesome-free" % "5.15.4",
  "org.webjars" %% "webjars-play" % playVersion.value
)

val testOutput = taskKey[Unit]("Tests whether the generated output matches the reference output")
testOutput := {
  val compiledFile = "target/web/public/main/stylesheets/base.css"
  val referenceFile = "reference.css"

  implicit val codec = Codec.UTF8
  val compiled = scala.io.Source.fromFile(compiledFile).mkString
  val reference = scala.io.Source.fromFile(referenceFile).mkString

  if (!compiled.equals(reference)) throw new Exception("Contents of the generated CSS doesn't match the reference")
}