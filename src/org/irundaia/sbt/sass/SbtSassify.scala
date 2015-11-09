package org.irundaia.sbt.sass

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.{SbtWeb, incremental}
import org.irundaia.sbt.sass.CssStyle.CssStyle
import sbt.Keys._
import sbt._

object SassKeys {
  val sassify = TaskKey[Seq[File]]("sassify", "Generate css files from scss and sass files.")

  val cssStyle = SettingKey[CssStyle]("cssStyle", "The style of the to-be-output CSS files.")
  val generateSourceMaps = SettingKey[Boolean]("generateSourceMaps", "Whether or not source map files should be generated.")
}

object SbtSassify extends AutoPlugin {
  override def requires: Plugins = SbtWeb
  override def trigger: PluginTrigger = AllRequirements
  import SassKeys._

  override lazy val buildSettings = Seq(
    cssStyle := CssStyle.Minified,
    generateSourceMaps := true)

  val baseSbtSassSettings = Seq(
    excludeFilter in sassify := HiddenFileFilter || "_*",
    includeFilter in sassify := "*.sass" || "*.scss",

    managedResourceDirectories += (resourceManaged in sassify in Assets).value,
    resourceManaged in sassify in Assets := webTarget.value / "sass" / "main",
    resourceGenerators in Assets <+= sassify in Assets,

    sassify in Assets := Def.task {
      val sourceDir = (sourceDirectory in Assets).value
      val targetDir = (resourceManaged in sassify in Assets).value
      val sources = (sourceDir ** ((includeFilter in sassify in Assets).value -- (excludeFilter in sassify in Assets).value)).get

      val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
        modifiedSources: Seq[File] =>

          if (modifiedSources.nonEmpty)
            streams.value.log.info(s"Sass compiling on ${modifiedSources.size} source(s)")

          // Compile all modified sources
          val compilationResults = modifiedSources
            .map(inputFile => inputFile ->
              new SassCompiler(CompilerSettings(cssStyle.value, generateSourceMaps.value))
                .compile(inputFile, baseDirectory.value, sourceDir, targetDir))
            .toMap

          // Collect the created files
          val createdFiles = compilationResults
            .values
            .map(_.filesWritten)
            .foldLeft(Seq.empty[File]){
              case (acc, addedFiles) => acc ++ addedFiles
            }

          if (createdFiles.nonEmpty)
            streams.value.log.info(s"Sass compilation done. ${createdFiles.size} resulting css/source map file(s)")

          (compilationResults, createdFiles)
      }

      // Return the dependencies
      (results._1 ++ results._2.toSet).toSeq
    }.dependsOn(WebKeys.webModules in Assets).value
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)
}