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

package org.irundaia.sass

import java.io.{File, FileWriter}
import java.nio.file.Path
import java.util.regex.Pattern
import org.irundaia.sass.jna.SassLibrary
import org.irundaia.sourcemap.SourceMapping
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object SassCompiler {
  def compile(sass: File, sourceDir: File, targetDir: File, compilerSettings: CompilerSettings): Try[CompilationResult] = {
    // Determine the source filename (relative to the source directory)
    val fileName = sass.getPath.replaceAll(Pattern.quote(sourceDir.getPath), "").replaceFirst("""\.\w+""", "")
    def sourceWithExtn(extn: String): File = new File(s"$targetDir$fileName.$extn")

    // Determine target files
    val css = sourceWithExtn("css")
    val sourceMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    css.getParentFile.mkdirs()

    // Compile the sources, and get the dependencies
    val output = doCompile(sass, css, sourceMap, compilerSettings)

    // Output the CSS or throw an exception when compilation failed
    output.map{case results =>
      outputCss(results, css)
      outputSourceMap(sass, sourceMap, results, sass.getParent, compilerSettings)
      determineCompilationDependencies(results, sass, css, sourceMap)
    }
  }

  def doCompile(source: File, target: File, map: File, compilerSettings: CompilerSettings): Try[Output] = {
    var context = Context(source.toPath)

    compilerSettings.applySettings(source, context.options)
    context.options.inputPath = source.toPath
    context.options.outputPath = target.toPath
    context.options.sourceMapPath = map.toPath

    val compileStatus = SassLibrary.INSTANCE.sass_compile_file_context(context.nativeContext)
    val output = Output(context)

    context.cleanup()

    if (compileStatus != 0)
      Failure(CompilerException(output))
    else
      Success(output)
  }

  private def outputCss(compilationResult: Output, css: File) = {
      val cssWriter = new FileWriter(css)

      cssWriter.write(compilationResult.css)
      cssWriter.flush()
      cssWriter.close()
    }

  private def outputSourceMap(source: File, sourceMap: File, output: Output, sourceDir: String, compilerSettings: CompilerSettings) =
    Option(output.sourceMap) match {
      case Some(sourceMapContent) if compilerSettings.generateSourceMaps =>
        val revisedMap = fixSourceMap(sourceMapContent, compilerSettings, source.getParent, sourceDir)
        val mapWriter = new FileWriter(sourceMap)

        mapWriter.write(revisedMap)
        mapWriter.flush()
        mapWriter.close()
      case _ => // Do not output any source map
    }

  def determineCompilationDependencies(compilationResult: Output, sass: File, css: File, sourceMap: File): CompilationResult = {
    val filesWritten = if (sourceMap.exists)
      Set(css, sourceMap)
    else
      Set(css)

    // Extract the file dependencies from the source map.
    Option(compilationResult.sourceMap) match {
      case Some(sourceMapContent) =>
        new CompilationResult(extractDependencies(css.getParent, sourceMapContent).filter(_.exists).toSet, filesWritten)
      case None =>
        new CompilationResult(Set(normalizeFile(sass)), filesWritten)
    }
  }

  private def extractDependencies(baseDir: String, originalSourceMap: String): Seq[File] =
    // Map to a file to get the normalized path because libsass does not use platform specific separators
    normalizeFiles(baseDir, (Json.parse(originalSourceMap) \ "sources")
      .as[Seq[String]])

  private def fixSourceMap(originalSourceMap: String, compilerSettings: CompilerSettings, baseDir: String, sourceDir: String): String = {
    val parsedSourceMap = Json.parse(originalSourceMap).as[JsObject]
    // Combine source file references with their contents
    val sources = normalizeFiles(baseDir, (parsedSourceMap \ "sources").as[Seq[String]])
    val sourcesWithContents = sources
      .zip((parsedSourceMap \ "sourcesContent").toOption.map(_.as[Seq[String]]).getOrElse(Stream.continually("")))
      .toMap
      .filterKeys(_.exists) // Filter non-existing sources

    // Exclude unknown files from the mappings
    val excludedSources = sources.zipWithIndex.toMap.filterKeys(!_.exists)
    val mappings = (parsedSourceMap \ "mappings").toOption.map(_.as[String]).getOrElse("")
    val mappingsWithoutExcludedSources = excludeMappings(SourceMapping.decode(mappings), excludedSources.values.toSet)

    // Use relative file names to make sure that the browser can find the files when they are moved to the target dir
    val transformedSources = sourcesWithContents.keys
        .map(convertToRelativePath(_, compilerSettings.includePaths))
        .map(JsString.apply)

    // Update the source map with the newly computed sources (contents)
    val updatedMap = if (compilerSettings.embedSources)
        parsedSourceMap ++
          Json.obj("sources" -> JsArray(transformedSources.toSeq)) ++
          Json.obj("mappings" -> JsString(mappingsWithoutExcludedSources)) ++
          Json.obj("sourcesContent" -> JsArray(sourcesWithContents.values.map(JsString).toSeq))
      else
        parsedSourceMap ++
          Json.obj("sources" -> JsArray(transformedSources.toSeq)) ++
          Json.obj("mappings" -> JsString(mappingsWithoutExcludedSources))

    Json.prettyPrint(updatedMap)
  }

  private def excludeMappings(mappings: Seq[SourceMapping], excludedFileIndices: Set[Int]): String = {
    val fileIndicesWithDeltas = mappings.map(_.sourceFileIndex).toSet[Int].map(fileIndex => (fileIndex, excludedFileIndices.count(_ < fileIndex))).toMap
    SourceMapping.encode(
      mappings
        .filterNot(mapping => excludedFileIndices.contains(mapping.sourceFileIndex))
        .map(mapping =>
          mapping.copy(
            sourceFileIndex = mapping.sourceFileIndex - fileIndicesWithDeltas(mapping.sourceFileIndex))))
  }

  private def normalizeFiles(baseDir: String, fileNames: Iterable[String]): Seq[File] =
    fileNames
      .map(f => normalizeFile(new File(s"""$baseDir/$f""")))
      .toSeq

  private def normalizeFile(f: File): File = f.toPath.normalize.toFile

  private def convertToRelativePath(file: File, includePaths: Iterable[Path]): String = {
    val normalizedPath = file.toPath.normalize.toString
    val ancestorDir = includePaths.find(includePath => normalizedPath.startsWith(includePath.toString))

    ancestorDir match {
      case None => normalizedPath
      case Some(ancestor) => normalizedPath.replaceFirst(Pattern.quote(ancestor + "/"), "")
    }
  }
}