import scala.io.Codec
import org.irundaia.sbt.sass._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

SassKeys.floatingPointPrecision := 5

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.webjars.npm" % "font-awesome" % "4.7.0",
  "org.webjars" %% "webjars-play" % "3.0.1"
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