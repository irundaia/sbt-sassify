/*
 * Copyright 2018 Han van Venrooij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.irundaia.sbt.sass

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import org.irundaia.sass._
import sbt.Keys._
import sbt._
import xsbti.{Problem, Severity}

object SbtSassify extends AutoPlugin {
  override def requires: Plugins = SbtWeb
  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    object SassKeys {
      val sassify = TaskKey[Seq[File]]("sassify", "Generate css files from scss and sass files.")
      val cssStyle = SettingKey[CssStyle]("cssStyle", "The style of the to-be-output CSS files.")
      val generateSourceMaps =
        SettingKey[Boolean]("generateSourceMaps", "Whether or not source map files should be generated.")
      val embedSources =
        SettingKey[Boolean]("embedSources", "Whether or not the source files should be embedded in the source map.")
      val syntaxDetection =
        SettingKey[SyntaxDetection]("syntaxDetection", "How to determine whether the sass/scss syntax is used.")
      val assetRootURL = SettingKey[String]("assetRootURL", "The base URL used to locate the assets.")
      val floatingPointPrecision = SettingKey[Int]("floatingPointPrecision", "The number of digits of precision used when rounding decimal numbers.")
    }
  }

  import autoImport.SassKeys._

  override lazy val buildSettings = Seq(
    cssStyle := Minified,
    generateSourceMaps := true,
    embedSources := true,
    syntaxDetection := Auto,
    assetRootURL := "/assets/",
    javaOptions += "-Djna.nosys=true",
    floatingPointPrecision := 10,
  )

  val baseSbtSassifySettings = Seq(
    excludeFilter in sassify := HiddenFileFilter || "_*",
    includeFilter in sassify := "*.sass" || "*.scss",

    managedResourceDirectories += (resourceManaged in sassify in Assets).value,
    resourceManaged in sassify in Assets := webTarget.value / "sass" / "main",
    resourceGenerators in Assets += sassify in Assets,

    sassify in Assets := Def.task {
      val sourceDir = (sourceDirectory in Assets).value
      val targetDir = (resourceManaged in sassify in Assets).value
      val webJarsDir = (webJarsDirectory in Assets).value
      val log = streams.value.log
      val sassReporter = (reporter in sassify).value

      val sources = (sourceDir ** ((includeFilter in sassify in Assets).value -- (excludeFilter in sassify in Assets).value)).get
      lazy val compilerSettings = CompilerSettings(
        cssStyle.value,
        generateSourceMaps.value,
        embedSources.value,
        syntaxDetection.value,
        Seq(sourceDir.toPath, webJarsDir.toPath),
        assetRootURL.value,
        floatingPointPrecision.value
      )

      implicit val fileHasherIncludingOptions: OpInputHasher[File] =
        OpInputHasher[File](f => OpInputHash.hashString(f.getCanonicalPath + compilerSettings.toString))

      val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
        modifiedSources: Seq[File] =>
          val startInstant = System.currentTimeMillis

          if (modifiedSources.nonEmpty)
            log.info(s"Sass compiling on ${modifiedSources.size} source(s)")

          // Compile all modified sources
          val compilationResults: Map[File, Either[CompilationFailure, CompilationSuccess]] = modifiedSources
            .map(inputFile => inputFile -> SassCompiler.compile(inputFile.toPath, sourceDir.toPath, targetDir.toPath, compilerSettings))
            .toMap

          // Collect OpResults
          val opResults: Map[File, OpResult] = compilationResults.mapValues {
            case Right(result) => OpSuccess(result.filesRead.map(_.toFile), result.filesWritten.map(_.toFile))
            case Left(_) => OpFailure
          }

          // Report compilation problems
          val problems: Seq[Problem] = compilationResults.collect {
            case (_, Left(e: LineBasedCompilationFailure)) =>
              new LineBasedProblem(
                e.message,
                Severity.Error,
                e.line,
                e.column,
                e.lineContent,
                e.source
              )
            case (f, Left(e)) =>
              new GeneralProblem(e.getMessage, f)
          }.toSeq
          CompileProblems.report(sassReporter, problems)

          // Collect the created files
          val createdFiles: Seq[File] = compilationResults
            .values
            .flatMap(_.right.toOption)
            .map(_.filesWritten)
            .foldLeft(Seq.empty[File]){
              case (acc, addedFiles) => acc ++ addedFiles.map(_.toFile)
            }

          val endInstant = System.currentTimeMillis

          if (createdFiles.nonEmpty)
            log.info(s"Sass compilation done in ${endInstant - startInstant} ms. ${createdFiles.size} resulting css/source map file(s)")

          (opResults, createdFiles)
      }(fileHasherIncludingOptions)

      // Return the dependencies
      (results._1 ++ results._2.toSet).toSeq
    }.dependsOn(WebKeys.webModules in Assets).value
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassifySettings)
}
