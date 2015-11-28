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

import java.io.{File, FileWriter}
import java.util.regex.Pattern

import com.typesafe.sbt.web.incremental.OpSuccess
import io.bit3.jsass.{Compiler, Options, OutputStyle}
import org.irundaia.sbt.sass._
import play.api.libs.json._

import scala.util.Try

class SassCompiler(compilerSettings: CompilerSettings) {

  def compile(source: File, baseDirectory: File, sourceDir: File, targetDir: File): Try[CompilationResult] = {
    // Determine the source filename (relative to the source directory)
    val fileName = source.getPath.replaceAll(Pattern.quote(sourceDir.getPath), "").replaceFirst("""\.\w+""", "")
    def sourceWithExtn(extn: String): File = new File(s"$targetDir$fileName.$extn")

    // Determine target files
    val targetCss = sourceWithExtn("css")
    val targetCssMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    targetCss.getParentFile.mkdirs()

    // Compile the sources, and get the dependencies
    Try(doCompile(
      source,
      targetCss,
      targetCssMap,
      baseDirectory.getAbsolutePath,
      source.getParent
    ))
  }


  def doCompile(in: File, out: File, map: File, baseDir: String, sourceDir: String): CompilationResult = {
    // Set compiler options
    val options = generateOptions(in, map)

    // Compile to CSS
    val compiler = new Compiler
    val compiled = compiler.compileFile(in.toURI, out.toURI, options)

    // Output the CSS or throw an exception when compilation failed
    Option(compiled.getCss) match {
      case Some(css) =>
        val cssWriter = new FileWriter(out)

        cssWriter.write(css)
        cssWriter.close()
      case _ => throw SassCompilerException(compiled)
    }

    // Output the source map in case it should be output
    Option(compiled.getSourceMap) match {
      case Some(sourceMap) if compilerSettings.generateSourceMaps =>
        val revisedMap = transformSourceMap(sourceMap, out.getParent, sourceDir)

        val mapWriter = new FileWriter(map)

        mapWriter.write(revisedMap)
        mapWriter.close()
      case _ => // Do not output any source map
    }

    val filesWritten = if (compilerSettings.generateSourceMaps)
      Set(out, map)
    else
      Set(out)

    // Extract the file dependencies from the source map.
    Option(compiled.getSourceMap) match {
      case Some(sourceMap) =>
        OpSuccess(extractDependencies(out.getParent, sourceMap).map(new File(_)), filesWritten)
      case None =>
        OpSuccess(Set(new File(in.getCanonicalPath)), filesWritten)
    }
  }

  private def generateOptions(sourceFile: File, sourceMapFile: File): Options = {
    val compilerStyle = compilerSettings.style match {
      case Minified => OutputStyle.COMPRESSED
      case Maxified => OutputStyle.EXPANDED
      case Sassy => OutputStyle.NESTED
    }

    // Determine syntax version
    val isIndented = compilerSettings.syntaxDetection match {
      case Auto => sourceFile.toString.endsWith("sass")
      case ForceSass => true
      case ForceScss => false
    }

    val options = new Options
    options.setSourceMapFile(sourceMapFile.toURI)
    // Note the source map will always be generated to determine the parsed files
    options.setOmitSourceMapUrl(!compilerSettings.generateSourceMaps)
    options.setSourceMapContents(compilerSettings.embedSources)
    options.setOutputStyle(compilerStyle)

    // Determine syntax version
    options.setIsIndentedSyntaxSrc(isIndented)

    options
  }

  private def extractDependencies(baseDir: String, originalMap: String): Set[String] = {
    val parsed = Json.parse(originalMap)
    (parsed \ "sources")
      .as[Set[String]]
      // Map to a file to get the canonical path because libsass does not use platform specific separators
      // Go up one directory because we want to get out of the target folder when retrieving the full path.
      .map(f => new File(s"""$baseDir../$f""").getCanonicalPath)
  }

  private def transformSourceMap(originalMap: String, baseDir: String, sourceDir: String): String = {
    val parsed = Json.parse(originalMap)
    // Use relative filenames to make sure that the bowser can find the files when they are moved
    val transformedDependencies = extractDependencies(baseDir, originalMap).map(convertToRelativePath(_, sourceDir))

    //
    val updatedMap =
      parsed.as[JsObject] ++
        Json.obj("sources" -> JsArray(transformedDependencies.map(JsString.apply).toSeq))

    Json.prettyPrint(updatedMap)
  }

  private def convertToRelativePath(fileName: String, sourceDir: String): String =
    fileName.replaceFirst(Pattern.quote(sourceDir + File.separator), "") // Convert to a relative path based on the sourceDir
}
