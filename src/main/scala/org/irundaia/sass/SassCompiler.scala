/*
 * Copyright 2016 Han van Venrooij
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

package org.irundaia.sass

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.irundaia.sass.jna.SassLibrary
import play.api.libs.json._

object SassCompiler {
  val charset = StandardCharsets.UTF_8

  def compile(sass: Path, sourceDir: Path, targetDir: Path, compilerSettings: CompilerSettings): Either[CompilationFailure, CompilationSuccess] = {
    // Determine the source filename (relative to the source directory)
    val targetSource = sourceDir.relativize(sass)
    def sourceWithExtn(extn: String): Path =
      targetDir.resolve(targetSource).resolveSibling(sass.getFileName.toString.replaceAll("""(.*\.)\w+""", s"""$$1$extn"""))

    // Determine target files
    val css = sourceWithExtn("css")
    val sourceMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    Files.createDirectories(css.getParent)

    // Compile the sources, and get the dependencies
    val eitherErrorOrOutput = doCompile(sass, css, sourceMap, compilerSettings)

    // Output the CSS and source map files
    eitherErrorOrOutput.right.foreach(output => {
        outputCss(output, css)
        outputSourceMap(sass, sourceMap, output, compilerSettings)
      }
    )

    // Return either the compilation error or the files read/written by the compiler
    eitherErrorOrOutput.right.map(output =>
      determineCompilationDependencies(output, sass, css, sourceMap)
    )
  }

  def doCompile(source: Path, target: Path, map: Path, compilerSettings: CompilerSettings): Either[CompilationFailure, Output] = {
    val context = Context(source)

    compilerSettings.applySettings(source, context.options)
    context.options.inputPath = source
    context.options.outputPath = target
    context.options.sourceMapPath = map

    val compileStatus = SassLibrary.INSTANCE.sass_compile_file_context(context.nativeContext)
    val output = Output(context)

    context.cleanup()

    if (compileStatus != 0)
      Left(CompilationFailure(output))
    else
      Right(output)
  }

  private def outputCss(compilationResult: Output, css: Path) = Files.write(css, compilationResult.css.getBytes(charset))

  private def outputSourceMap(source: Path, sourceMap: Path, output: Output, compilerSettings: CompilerSettings) =
    Option(output.sourceMap) match {
      case Some(sourceMapContent) if compilerSettings.generateSourceMaps =>
        val revisedMap = fixSourceMap(sourceMapContent, compilerSettings, sourceMap.getParent)
        Files.write(sourceMap, revisedMap.getBytes(charset))
      case _ => // Do not output any source map
    }

  def determineCompilationDependencies(compilationResult: Output, sass: Path, css: Path, sourceMap: Path): CompilationSuccess = {
    val filesWritten = if (Files.exists(sourceMap))
      Set(css, sourceMap)
    else
      Set(css)

    val filesRead = compilationResult.readFiles.map(Paths.get(_)).toSet

    CompilationSuccess(filesRead, filesWritten)
  }

  private def fixSourceMap(originalSourceMap: String, compilerSettings: CompilerSettings, baseDir: Path): String = {
    val parsedSourceMap = Json.parse(originalSourceMap).as[SourceMap]

    // Use relative file names to make sure that the browser can find the files when they are moved to the target dir
    val transformedSources =
      normalizeFiles(baseDir, parsedSourceMap.sources) // Get absolute paths
        .map(convertToRelativePath(_, compilerSettings.includePaths).toString) // Transform to relative paths relative to the include paths

    // Update the source map with the newly computed sources (contents)
    val updatedMap = parsedSourceMap.copy(sources = transformedSources)

    Json.prettyPrint(Json.toJson(updatedMap))
  }

  private def normalizeFiles(baseDir: Path, fileNames: Iterable[String]): Seq[Path] =
    fileNames
      .map(p => baseDir.resolve(p).normalize)
      .toSeq

  private def convertToRelativePath(path: Path, includePaths: Iterable[Path]): Path = {
    val normalizedPath = path.normalize
    val ancestorDir = includePaths.find(includePath => normalizedPath.startsWith(includePath))

    ancestorDir match {
      case None => normalizedPath
      case Some(ancestor) => ancestor.relativize(normalizedPath)
    }
  }
}
