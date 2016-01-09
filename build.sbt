name := "sbt-sassify"
organization := "org.irundaia.sbt"
sbtPlugin := true

fork in Test := true

javaOptions += "-Djna.nosys=true"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.2")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.4",
  "com.nativelibs4java" % "jnaerator-runtime" % "0.12",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)