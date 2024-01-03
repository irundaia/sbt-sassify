lazy val root = (project in file(".")).enablePlugins(SbtWeb)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.webjars.bower" % "bootstrap-sass" % "3.3.6",
  "org.webjars" %% "webjars-play" % "3.0.1",
)
