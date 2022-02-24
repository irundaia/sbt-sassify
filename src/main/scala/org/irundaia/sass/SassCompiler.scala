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

package org.irundaia.sass

import org.irundaia.util.extensions.RichPath

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

case class Location(file: Path, line: Int, column: Int)
case class LogMessage(level: Severity, message: String, location: Option[Location])
sealed trait Severity

object Severity {
  case object Warning extends Severity
  case object Debug extends Severity
  case object Error extends Severity
}

case class CompilationSuccess(filesRead: Set[Path], filesWritten: Set[Path], logMessages: Seq[LogMessage] = Seq())
case class CompilationFailure(logMessages: Seq[LogMessage])

trait SassCompiler {
  import SassCompiler._

  def compile(sass: Seq[Path], sourceDir: Path, targetDir: Path):
    Map[Path, Either[CompilationFailure, CompilationSuccess]]

  protected def ensurePathExists(path: Path): Path =
    Files.createDirectories(path)

  protected def determineOutputPaths(sourceDir: Path, targetDir: Path, source: Path, extension: String): (Path, Path) = {
    val relativeCssPath = sourceDir.relativize(source).withExtension(extension)
    val relativeSourceMapPath = relativeCssPath.withExtension(s"$extension.map")

    (targetDir.resolve(relativeCssPath), targetDir.resolve(relativeSourceMapPath))
  }

  protected def outputCss(css: String, target: Path): Path =
    Files.write(target, css.getBytes(charset))

  protected def outputSourceMap(sourceMap: String, target: Path): Path =
    Files.write(target, sourceMap.getBytes(charset))
}
object SassCompiler {
  private val charset = StandardCharsets.UTF_8
}
