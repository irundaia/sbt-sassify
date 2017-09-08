lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars.bower" % "bootstrap-sass" % "3.3.6",
  "org.webjars" %% "webjars-play" % "2.6.2"
)
