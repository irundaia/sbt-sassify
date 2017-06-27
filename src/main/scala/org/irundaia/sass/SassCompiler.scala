/*
 * Copyright 2017 Han van Venrooij
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
import java.nio.file.{Files, Path, Paths}

import org.irundaia.sass.jna.SassLibrary
import org.irundaia.util.extensions._

object SassCompiler {
  val charset = StandardCharsets.UTF_8

  def compile(sass: Path, sourceDir: Path, targetDir: Path, compilerSettings: CompilerSettings): Either[CompilationFailure, CompilationSuccess] = {
    def sourceWithExtn(extension: String): Path =
      targetDir.resolve(sourceDir.relativize(sass)).resolveSibling(sass.withExtension(extension).getFileName)

    // Determine target files
    val css = sourceWithExtn("css")
    val sourceMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    Files.createDirectories(css.getParent)

    // Compile the sources, and get the dependencies
    val eitherErrorOrOutput = doCompile(sass, compilerSettings)

    // Output the CSS and source map files
    eitherErrorOrOutput.right.foreach(output => {
        outputCss(output, css)
        outputSourceMap(sass, sourceMap, output, compilerSettings)
      }
    )

    // Return either the compilation error or the files read/written by the compiler
    eitherErrorOrOutput
        .fold(
          error => Left(CompilationFailure(error)),
          output => Right(determineCompilationDependencies(output, sass, css, sourceMap)))
  }

  def doCompile(source: Path, compilerSettings: CompilerSettings): Either[SassError, SassOutput] = {
    val context = Context(source)

    compilerSettings.applySettings(source, context.options)
    context.options.inputPath = source
    context.options.outputPath = source.withExtension("css")
    context.options.sourceMapPath = source.withExtension("css.map")

    SassLibrary.INSTANCE.sass_compile_file_context(context.nativeContext)

    val result = Output(context) match {
      case e: SassError => Left(e)
      case o: SassOutput => Right(o)
    }

    context.cleanup()

    result
  }

  private def outputCss(compilationResult: SassOutput, css: Path) = Files.write(css, compilationResult.css.getBytes(charset))

  private def outputSourceMap(source: Path, sourceMap: Path, output: SassOutput, compilerSettings: CompilerSettings) =
    Option(output.sourceMap) match {
      case Some(sourceMapContent) if compilerSettings.generateSourceMaps =>
        Files.write(sourceMap, sourceMapContent.getBytes(charset))
      case _ => // Do not output any source map
    }

  def determineCompilationDependencies(compilationResult: SassOutput, sass: Path, css: Path, sourceMap: Path): CompilationSuccess = {
    val filesWritten = if (Files.exists(sourceMap))
      Set(css, sourceMap)
    else
      Set(css)

    val filesRead = compilationResult.readFiles.map(Paths.get(_)).toSet

    CompilationSuccess(filesRead, filesWritten)
  }
}
