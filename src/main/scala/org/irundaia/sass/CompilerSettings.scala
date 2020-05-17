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

package org.irundaia.sass

import java.nio.file.Path

case class CompilerSettings(
     outputStyle: CssStyle,
     generateSourceMaps: Boolean,
     embedSources: Boolean,
     syntaxDetection: SyntaxDetection,
     includePaths: Seq[Path],
     sourceMapRoot: String,
     precision: Int,
     extension: String) {

  def applySettings(sourceFile: Path, options: Options): Unit = {
    options.indentedSyntaxSrc = syntaxDetection match {
      case Auto => sourceFile.toString.endsWith("sass")
      case ForceSass => true
      case ForceScss => false
    }
    options.outputStyle = outputStyle
    options.omitSourceMapUrl = !generateSourceMaps
    options.sourceMapContents = embedSources
    options.includePaths ++= includePaths
    options.sourceMapRoot = sourceMapRoot
    options.precision = precision
  }
}
