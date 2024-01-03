import org.irundaia.sbt.sass._

lazy val root = (project in file(".")).enablePlugins(SbtWeb).settings(
  Assets / sourceDirectory := (Compile / sourceDirectory).value / "foo.bar.com" / "assets",
  SassKeys.embedSources := true
)