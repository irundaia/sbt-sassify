import org.irundaia.sbt.sass._

lazy val root = (project in file(".")).enablePlugins(SbtWeb).settings(
  sourceDirectory in Assets := (sourceDirectory in Compile).value / "foo.bar.com" / "assets",
  SassKeys.embedSources := true
)