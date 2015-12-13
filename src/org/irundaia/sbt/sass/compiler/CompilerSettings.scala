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

package org.irundaia.sbt.sass.compiler

import java.io.File

import io.bit3.jsass.{Options, OutputStyle}
import org.irundaia.sbt.sass._
import scala.collection.convert.wrapAsJava

case class CompilerSettings(style: CssStyle, generateSourceMaps: Boolean, embedSources: Boolean, syntaxDetection: SyntaxDetection, includeDirs: Seq[File]) {

  def compilerStyle: OutputStyle = style match {
    case Minified => OutputStyle.COMPRESSED
    case Maxified => OutputStyle.EXPANDED
    case Sassy => OutputStyle.NESTED
  }

  def isIndented(fileName: String): Boolean = syntaxDetection match {
    case Auto => fileName.endsWith("sass")
    case ForceSass => true
    case ForceScss => false
  }

  def toCompilerOptions(sourceFile: File, sourceMapFile: File): Options = {
    val options = new Options

    options.setSourceMapFile(sourceMapFile.toURI)
    // Note the source map will always be generated to determine the parsed files
    options.setOmitSourceMapUrl(!generateSourceMaps)
    options.setSourceMapContents(embedSources)
    options.setOutputStyle(compilerStyle)
    options.setIsIndentedSyntaxSrc(isIndented(sourceFile.toString))
    options.getIncludePaths.addAll(wrapAsJava.seqAsJavaList(includeDirs))

    options
  }
}