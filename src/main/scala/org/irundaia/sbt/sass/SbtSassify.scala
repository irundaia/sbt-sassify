/*
 * Copyright 2022 Han van Venrooij
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

import com.typesafe.sbt.web.Import.WebKeys.*
import com.typesafe.sbt.web.SbtWeb.autoImport.*
import com.typesafe.sbt.web.*
import com.typesafe.sbt.web.incremental.*
import org.irundaia.sass.dart.DartSassCompiler
import org.irundaia.sass.libsass.LibSassCompiler
import org.irundaia.sass.{CompilationFailure, CompilationSuccess, CompilerSettings, CssStyle, SyntaxDetection, Severity as SassSeverity}
import org.irundaia.util.extensions.RichPath
import sbt.Keys.*
import sbt.*
import xsbti.{Problem, Severity}

import java.nio.file.Path

object SbtSassify extends AutoPlugin {
  override def requires: Plugins = SbtWeb
  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    object SassKeys {
      val sassify = TaskKey[Seq[File]]("sassify", "Generate css files from scss and sass files.")
      val cssStyle = SettingKey[CssStyle]("cssStyle", "The style of the to-be-output CSS files.")
      val useDartSass = SettingKey[Boolean]("useDartSass", "Whether or not to use the dart sass compiler.")
      val dartSassDir = SettingKey[Option[File]](
        "dartSassDir",
        "The directory that is used by sbt-sassify to run the dart sass compiler. Make sure that executables in this file system are allowed to be run."
      )
      val generateSourceMaps =
        SettingKey[Boolean]("generateSourceMaps", "Whether or not source map files should be generated.")
      val embedSources =
        SettingKey[Boolean]("embedSources", "Whether or not the source files should be embedded in the source map.")
      val syntaxDetection =
        SettingKey[SyntaxDetection]("syntaxDetection", "How to determine whether the sass/scss syntax is used.")
      val assetRootURL = SettingKey[String]("assetRootURL", "The base URL used to locate the assets.")
      val floatingPointPrecision = SettingKey[Int]("floatingPointPrecision", "The number of digits of precision used when rounding decimal numbers.")
      val extension = SettingKey[String]("extension", "The file extension to be used for the compiled file")
    }
  }

  import autoImport.SassKeys.*

  override lazy val buildSettings = Seq(
    cssStyle := Minified,
    useDartSass := false,
    dartSassDir := None,
    generateSourceMaps := true,
    embedSources := true,
    syntaxDetection := Auto,
    assetRootURL := "/assets/",
    javaOptions += "-Djna.nosys=true",
    floatingPointPrecision := 10,
    extension := "css"
  )

  val baseSbtSassifySettings = Seq(
    sassify / excludeFilter := HiddenFileFilter || "_*",
    sassify / includeFilter := "*.sass" || "*.scss",

    managedResourceDirectories += (Assets / sassify / resourceManaged).value,
    Assets / sassify / resourceManaged := webTarget.value / "sass" / "main",
    Assets / resourceGenerators += Assets / sassify,

    Assets / sassify := Def.task {
      val sourceDir = (Assets / sourceDirectory).value
      val targetDir = (Assets / sassify / resourceManaged).value
      val webJarsDir = (Assets / webJarsDirectory).value
      val log = streams.value.log
      val sassReporter = (sassify / reporter).value

      val sources = (sourceDir ** ((Assets / sassify / includeFilter).value -- (Assets / sassify / excludeFilter).value)).get
      val compilerSettings = CompilerSettings(
        cssStyle.value,
        generateSourceMaps.value,
        embedSources.value,
        syntaxDetection.value,
        Seq(sourceDir.toPath, webJarsDir.toPath),
        assetRootURL.value,
        floatingPointPrecision.value,
        extension.value,
        dartSassDir.value.map(_.toPath)
      )
      val compiler =
        if (useDartSass.value) DartSassCompiler(compilerSettings)
        else LibSassCompiler(compilerSettings)

      implicit val fileHasherIncludingOptions: OpInputHasher[File] =
        OpInputHasher[File](f => OpInputHash.hashString(f.getCanonicalPath + compilerSettings.toString + useDartSass.value))

      val results = incremental.syncIncremental((Assets / streams).value.cacheDirectory / "run", sources) {
        modifiedSources: Seq[File] =>
          val startInstant = System.currentTimeMillis

          if (modifiedSources.nonEmpty)
            log.info(s"Sass compiling on ${modifiedSources.size} source(s)")

          // Compile all modified sources
          val compilationResults: Map[Path, Either[CompilationFailure, CompilationSuccess]] =
            compiler.compile(modifiedSources.map(_.toPath), sourceDir.toPath, targetDir.toPath)

          // Collect OpResults
          val opResults: Map[File, OpResult] = compilationResults.map {
            case (p, Right(result)) => (p.toFile, OpSuccess(result.filesRead.map(_.toFile), result.filesWritten.map(_.toFile)))
            case (p, Left(_)) => (p.toFile, OpFailure)
          }

          // Report compilation problems
          val problems: Seq[Problem] = compilationResults.mapValues(_.fold(_.logMessages, _.logMessages))
            .flatMap { case (path, messages) =>
              messages.map(message => (path, message, message.location.map(location => location.file.line(location.line))))
            }
            .map { case (path, logMessage, lineContent) =>
              logMessage.location
                .map(location => new LineBasedProblem(
                  logMessage.message,
                  logMessage.level match {
                    case SassSeverity.Warning => Severity.Warn
                    case SassSeverity.Debug => Severity.Info
                    case SassSeverity.Error => Severity.Error
                  },
                  location.line,
                  location.column,
                  lineContent.getOrElse(""),
                  path.toFile
                ))
                .getOrElse(new GeneralProblem(logMessage.message, path.toFile) {
                  override def severity(): Severity =
                    logMessage.level match {
                      case SassSeverity.Warning => Severity.Warn
                      case SassSeverity.Debug => Severity.Info
                      case SassSeverity.Error => Severity.Error
                    }
                })
            }
            .toSeq
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
    }.dependsOn(Assets / WebKeys.webModules).value
  )

  override def projectSettings: Seq[Setting[?]] = inConfig(Assets)(baseSbtSassifySettings)
}
