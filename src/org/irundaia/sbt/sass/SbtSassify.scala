/*
 * Copyright 2015 Han van Venrooij
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
import com.typesafe.sbt.web.incremental.{OpInputHash, OpInputHasher, OpResult, OpFailure}
import com.typesafe.sbt.web.{LineBasedProblem, CompileProblems, SbtWeb, incremental}
import org.irundaia.sbt.sass.compiler.{SassCompilerException, SassCompiler, CompilerSettings}
import sbt.Keys._
import sbt._
import xsbti.{Severity, Problem}

import scala.language.implicitConversions
import scala.util.{Try, Success, Failure}

object SassKeys {
  val sassify = TaskKey[Seq[File]]("sassify", "Generate css files from scss and sass files.")

  val cssStyle = SettingKey[CssStyle]("cssStyle", "The style of the to-be-output CSS files.")
  val generateSourceMaps =
    SettingKey[Boolean]("generateSourceMaps", "Whether or not source map files should be generated.")
  val embedSources =
    SettingKey[Boolean]("embedSources", "Whether or not the source files should be embedded in the source map")
  val syntaxDetection =
    SettingKey[SyntaxDetection]("syntaxDetection", "How to determine whether the sass/scss syntax is used")
}

object SbtSassify extends AutoPlugin {
  override def requires: Plugins = SbtWeb
  override def trigger: PluginTrigger = AllRequirements
  import SassKeys._

  override lazy val buildSettings = Seq(
    cssStyle := Minified,
    generateSourceMaps := true,
    embedSources := true,
    syntaxDetection := Auto
  )

  val baseSbtSassifySettings = Seq(
    excludeFilter in sassify := HiddenFileFilter || "_*",
    includeFilter in sassify := "*.sass" || "*.scss",

    managedResourceDirectories += (resourceManaged in sassify in Assets).value,
    resourceManaged in sassify in Assets := webTarget.value / "sass" / "main",
    resourceGenerators in Assets <+= sassify in Assets,

    sassify in Assets := Def.task {
      val sourceDir = (sourceDirectory in Assets).value
      val targetDir = (resourceManaged in sassify in Assets).value
      val webJarsDir = (webJarsDirectory in Assets).value

      val sources = (sourceDir ** ((includeFilter in sassify in Assets).value -- (excludeFilter in sassify in Assets).value)).get
      lazy val compilerSettings =
        CompilerSettings(cssStyle.value, generateSourceMaps.value, embedSources.value, syntaxDetection.value, Seq(sourceDir, webJarsDir))

      implicit val fileHasherIncludingOptions: OpInputHasher[File] =
        OpInputHasher[File](f => OpInputHash.hashString(f.getCanonicalPath + compilerSettings.toString))

      val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
        modifiedSources: Seq[File] =>
          if (modifiedSources.nonEmpty)
            streams.value.log.info(s"Sass compiling on ${modifiedSources.size} source(s)")

          // Compile all modified sources
          val compiler = new SassCompiler(compilerSettings)
          val compilationResults: Map[File, Try[CompilationResult]] = modifiedSources
            .map(inputFile => inputFile -> compiler.compile(inputFile, sourceDir, targetDir))
            .toMap

          // Collect OpResults
          val opResults: Map[File, OpResult] = compilationResults.mapValues {
            case Success(compResult) => compResult // Note that a CompilationResult is a OpSuccess
            case Failure(_) => OpFailure
          }

          // Report compilation problems
          val problems: Seq[Problem] = compilationResults.values.collect {
            case Failure(e: SassCompilerException) =>
              new LineBasedProblem(
                e.message,
                Severity.Error,
                e.line,
                e.column,
                e.lineContent,
                e.source
              )
          }.toSeq
          CompileProblems.report((reporter in sassify).value, problems)

          // Collect the created files
          val createdFiles: Seq[File] = compilationResults
            .values
            .flatMap(_.toOption)
            .map(_.filesWritten)
            .foldLeft(Seq.empty[File]){
              case (acc, addedFiles) => acc ++ addedFiles
            }

          if (createdFiles.nonEmpty)
            streams.value.log.info(s"Sass compilation done. ${createdFiles.size} resulting css/source map file(s)")

          (opResults, createdFiles)
      }(fileHasherIncludingOptions)

      // Return the dependencies
      (results._1 ++ results._2.toSet).toSeq
    }.dependsOn(WebKeys.webModules in Assets).value
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassifySettings)
}