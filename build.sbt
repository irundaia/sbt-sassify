name := "sbt-sassify"
organization := "org.irundaia.sbt"
sbtPlugin := true

fork in Test := true

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.0")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "net.java.dev.jna" % "jna" % "4.2.2"
)
