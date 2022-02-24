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

package org.irundaia.sass.libsass

import com.sun.jna.{Native, Pointer}
import org.irundaia.sass.jna.{SassLibrary, SizeT}
import org.irundaia.sass.jna.SassLibrary.Sass_Function_Fn
import org.irundaia.sass.{Auto, CompilationFailure, CompilationSuccess, CompilerSettings, ForceSass, ForceScss, Location, LogMessage, SassCompiler, Severity}
import org.irundaia.util.extensions.RichPath

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable

case class LibSassCompiler(compilerSettings: CompilerSettings) extends SassCompiler {
  import org.irundaia.sass.libsass.LibSassCompiler._

  override def compile(sass: Seq[Path],
                       sourceDir: Path,
                       targetDir: Path): Map[Path, Either[CompilationFailure, CompilationSuccess]] =
    sass.map(input => input -> compile(input, sourceDir, targetDir)).toMap

  def compile(sass: Path, sourceDir: Path, targetDir: Path): Either[CompilationFailure, CompilationSuccess] = {
    // Determine target files
    val (css, sourceMap) =
      determineOutputPaths(sourceDir, targetDir, sass, compilerSettings.extension)

    // Compile the sources, and get the dependencies
    val (logMessages, eitherErrorOrOutput) = doCompile(sass, compilerSettings)

    // Output the CSS and source map files
    eitherErrorOrOutput.right
      .foreach(output => {
        // Make sure that the target directory is created
        ensurePathExists(css.getParent)

        outputCss(output.css, css)
        if (compilerSettings.generateSourceMaps)
          outputSourceMap(output.sourceMap, sourceMap)
      })

    // Return either the compilation error or the files read/written by the compiler
    eitherErrorOrOutput
      .fold(
        error => Left(CompilationFailure(error.toMessage +: logMessages)),
        output => {
          val (filesRead, filesWritten) = determineCompilationDependencies(output, css, sourceMap)
          Right(CompilationSuccess(filesRead, filesWritten, logMessages))
        })
  }

  private def doCompile(source: Path, compilerSettings: CompilerSettings): (Seq[LogMessage], Either[SassError, SassOutput]) = {
    val context = Context(source)

    val logBuffer = compilerSettings.applySettings(source, context.options)
    context.options.inputPath = source
    context.options.outputPath = source.withExtension(compilerSettings.extension)
    context.options.sourceMapPath = source.withExtension(s"${compilerSettings.extension}.map")

    libraryInstance.sass_compile_file_context(context.nativeContext)

    val result = Output(context) match {
      case e: SassError => Left(e)
      case o: SassOutput => Right(o)
    }

    context.cleanup()

    (logBuffer, result)
  }

  private def determineCompilationDependencies(compilationResult: SassOutput, css: Path, sourceMap: Path): (Set[Path], Set[Path]) = {
    val filesWritten = if (Files.exists(sourceMap))
      Set(css, sourceMap)
    else
      Set(css)

    val filesRead = compilationResult.readFiles.map(Paths.get(_)).toSet

    (filesRead, filesWritten)
  }
}

object LibSassCompiler {
  private val library = "sass"
  val libraryInstance: SassLibrary = Native.load(library, classOf[SassLibrary])

  implicit private class RichCompilerSettings(compilerSettings: CompilerSettings) {
    def applySettings(sourceFile: Path, options: Options): mutable.Buffer[LogMessage] = {
      options.indentedSyntaxSrc = compilerSettings.syntaxDetection match {
        case Auto => sourceFile.toString.endsWith("sass")
        case ForceSass => true
        case ForceScss => false
      }
      options.outputStyle = compilerSettings.outputStyle
      options.omitSourceMapUrl = !compilerSettings.generateSourceMaps
      options.sourceMapContents = compilerSettings.embedSources
      options.includePaths ++= compilerSettings.includePaths
      options.sourceMapRoot = compilerSettings.sourceMapRoot
      options.precision = compilerSettings.precision

      val logBuffer = collection.mutable.Buffer[LogMessage]()

      val buffer: Severity => Sass_Function_Fn = (severity: Severity) => (args: SassLibrary.Sass_Value, _, compiler: SassLibrary.Sass_Compiler) => {
        val firstArg = LibSassCompiler.libraryInstance.sass_list_get_value(args, new SizeT(0))

        val message = LibSassCompiler.libraryInstance.sass_string_get_value(firstArg)
        val location = Some(LibSassCompiler.libraryInstance.sass_compiler_get_last_callee(compiler))
          .filter(_.getPointer == Pointer.NULL)
          .map(callee => {
            val file = LibSassCompiler.libraryInstance.sass_callee_get_path(callee)
            val line = LibSassCompiler.libraryInstance.sass_callee_get_line(callee).intValue()
            val column = LibSassCompiler.libraryInstance.sass_callee_get_column(callee).intValue() - 1

            Location(Paths.get(file), line, column)
          })

        logBuffer.append(LogMessage(
          severity,
          message,
          location
        ))
        LibSassCompiler.libraryInstance.sass_make_null()
      }

      val debugFunction: SassLibrary.Sass_Function_Entry =
        LibSassCompiler.libraryInstance.sass_make_function("@debug", buffer(Severity.Debug), null) // scalastyle:ignore
      val warnFunction: SassLibrary.Sass_Function_Entry =
        LibSassCompiler.libraryInstance.sass_make_function("@warn", buffer(Severity.Warning), null) // scalastyle:ignore

      val functionList = LibSassCompiler.libraryInstance.sass_make_function_list(new SizeT(2))
      LibSassCompiler.libraryInstance.sass_function_set_list_entry(functionList, new SizeT(0), debugFunction)
      LibSassCompiler.libraryInstance.sass_function_set_list_entry(functionList, new SizeT(1), warnFunction)

      LibSassCompiler.libraryInstance.sass_option_set_c_functions(options.nativeOptions, functionList)

      logBuffer
    }
  }
}
