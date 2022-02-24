lazy val root = (project in file(".")).enablePlugins(SbtWeb)

lazy val playVersion = settingKey[String]("Play version in relation to current Scala Version")

playVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => "2.4.0-2"
    case Some((2, n)) if n > 10 => "2.8.8"
  }
}

libraryDependencies ++= Seq(
  "org.webjars.bower" % "bootstrap-sass" % "3.3.6",
  "org.webjars" %% "webjars-play" % playVersion.value
)
