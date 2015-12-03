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
import io.bit3.jsass.{Output, Compiler, Options, OutputStyle}
import org.irundaia.sbt.sass._
import play.api.libs.json._

import scala.util.Try

class SassCompiler(compilerSettings: CompilerSettings) {

  def compile(sass: File, sourceDir: File, targetDir: File): Try[CompilationResult] = {
    // Determine the source filename (relative to the source directory)
    val fileName = sass.getPath.replaceAll(Pattern.quote(sourceDir.getPath), "").replaceFirst("""\.\w+""", "")
    def sourceWithExtn(extn: String): File = new File(s"$targetDir$fileName.$extn")

    // Determine target files
    val css = sourceWithExtn("css")
    val sourceMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    css.getParentFile.mkdirs()

    // Compile the sources, and get the dependencies
    Try(doCompile(
      sass,
      css,
      sourceMap
    ))
  }

  def doCompile(sass: File, css: File, sourceMap: File): CompilationResult = {
    val options = generateCompilerOptions(sass, sourceMap)

    // Compile to CSS
    val compiler = new Compiler
    val compilationResult = compiler.compileFile(sass.toURI, css.toURI, options)

    // Output the CSS or throw an exception when compilation failed
    outputCss(compilationResult, css)
    outputSourceMap(compilationResult, css, sourceMap, sass.getParent)

    determineCompilationDependencies(compilationResult, sass, css, sourceMap)
  }

  private def generateCompilerOptions(sourceFile: File, sourceMapFile: File): Options = {
    val options = new Options

    options.setSourceMapFile(sourceMapFile.toURI)
    // Note the source map will always be generated to determine the parsed files
    options.setOmitSourceMapUrl(!compilerSettings.generateSourceMaps)
    options.setSourceMapContents(compilerSettings.embedSources)
    options.setOutputStyle(compilerSettings.compilerStyle)
    options.setIsIndentedSyntaxSrc(compilerSettings.isIndented(sourceFile.toString))

    options
  }

  private def outputCss(compilationResult: Output, css: File) =
    Option(compilationResult.getCss) match {
      case Some(cssContent) =>
        val cssWriter = new FileWriter(css)

        cssWriter.write(cssContent)
        cssWriter.close()
      case _ => throw SassCompilerException(compilationResult)
    }

  private def outputSourceMap(compilationResult: Output, css: File, sourceMap: File, sourceDir: String) =
    Option(compilationResult.getSourceMap) match {
      case Some(sourceMapContent) if compilerSettings.generateSourceMaps =>
        val revisedMap = transformSourceMap(sourceMapContent, css.getParent, sourceDir)
        val mapWriter = new FileWriter(sourceMap)

        mapWriter.write(revisedMap)
        mapWriter.close()
      case _ => // Do not output any source map
    }

  def determineCompilationDependencies(compilationResult: Output, sass: File, css: File, sourceMap: File): CompilationResult = {
    val filesWritten = if (compilerSettings.generateSourceMaps)
      Set(css, sourceMap)
    else
      Set(css)

    // Extract the file dependencies from the source map.
    Option(compilationResult.getSourceMap) match {
      case Some(sourceMapContent) =>
        OpSuccess(extractDependencies(css.getParent, sourceMapContent).filter(_.exists).toSet, filesWritten)
      case None =>
        OpSuccess(Set(normalizeFile(sass)), filesWritten)
    }
  }

  private def extractDependencies(baseDir: String, originalSourceMap: String): Seq[File] =
    // Map to a file to get the normalized path because libsass does not use platform specific separators
    normalizeFiles(baseDir, (Json.parse(originalSourceMap) \ "sources")
      .as[Seq[String]])

  private def normalizeFiles(baseDir: String, fileNames: Iterable[String]): Seq[File] =
    fileNames
      .map(f => normalizeFile(new File(s"""$baseDir/$f""")))
      .toSeq

  private def normalizeFile(f: File): File = f.toPath.normalize().toFile

  private def transformSourceMap(originalMap: String, baseDir: String, sourceDir: String): String = {
    val parsed = Json.parse(originalMap).as[JsObject]
    val sourcesContent = (parsed \ "sourcesContent").toOption.map(_.as[Seq[String]])
    val hasSourcesContent = sourcesContent.isDefined
    val sourcesWithContent = normalizeFiles(baseDir, (parsed \ "sources").as[Seq[String]])
      .zip(sourcesContent.getOrElse(Stream.continually("")))
      .toMap
    val filteredSources = sourcesWithContent.filterKeys(_.exists)

    // Use relative file names to make sure that the browser can find the files when they are moved to the target dir
    val transformedDependencies = filteredSources.keys.toSeq
        .map(convertToRelativePath(_, sourceDir))
        .map(JsString.apply)

    // Update the source map with the newly computed sources
    val mapWithUpdatedSources =
      parsed ++
        Json.obj("sources" -> JsArray(transformedDependencies))

    // Update the source map with the newly computed sources contents
    val updatedMap = if (hasSourcesContent)
        mapWithUpdatedSources ++
          Json.obj("sourcesContent" -> JsArray(filteredSources.values.map(JsString).toSeq))
      else
        mapWithUpdatedSources

    Json.prettyPrint(updatedMap)
  }

  private def convertToRelativePath(file: File, sourceDir: String): String =
    file.getCanonicalPath.replaceFirst(Pattern.quote(sourceDir + File.separator), "")
}