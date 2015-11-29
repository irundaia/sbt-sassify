name := "sbt-sassify"
organization := "org.irundaia.sbt"
sbtPlugin := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.2")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.4",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "io.bit3" % "jsass" % "3.3.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
