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

import java.nio.file.{Files, Path}
import org.irundaia.sass.jna.SassLibrary
import org.irundaia.sourcemap.SourceMapping
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object SassCompiler {
  def compile(sass: Path, sourceDir: Path, targetDir: Path, compilerSettings: CompilerSettings): CompilationResult = {
    // Determine the source filename (relative to the source directory)
    val fileName = sourceDir.relativize(sass).toString.replaceFirst("""\.\w+$""", "")
    def sourceWithExtn(extn: String): Path = targetDir.resolve(s"$fileName.$extn")

    // Determine target files
    val css = sourceWithExtn("css")
    val sourceMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    Files.createDirectories(css.getParent)

    // Compile the sources, and get the dependencies
    val output = doCompile(sass, css, sourceMap, compilerSettings)

    // Output the CSS or throw an exception when compilation failed
    output.map{case results =>
      outputCss(results, css)
      outputSourceMap(sass, sourceMap, results, compilerSettings)
      determineCompilationDependencies(results, sass, css, sourceMap)
    }
  }

  def doCompile(source: Path, target: Path, map: Path, compilerSettings: CompilerSettings): Try[Output] = {
    val context = Context(source)

    compilerSettings.applySettings(source, context.options)
    context.options.inputPath = source
    context.options.outputPath = target
    context.options.sourceMapPath = map

    val compileStatus = SassLibrary.INSTANCE.sass_compile_file_context(context.nativeContext)
    val output = Output(context)

    context.cleanup()

    if (compileStatus != 0)
      Failure(CompilerException(output))
    else
      Success(output)
  }

  private def outputCss(compilationResult: Output, css: Path) = Files.write(css, compilationResult.css.getBytes)

  private def outputSourceMap(source: Path, sourceMap: Path, output: Output, compilerSettings: CompilerSettings) =
    Option(output.sourceMap) match {
      case Some(sourceMapContent) if compilerSettings.generateSourceMaps =>
        val revisedMap = fixSourceMap(sourceMapContent, compilerSettings, sourceMap.getParent)
        Files.write(sourceMap, revisedMap.getBytes)
      case _ => // Do not output any source map
    }

  def determineCompilationDependencies(compilationResult: Output, sass: Path, css: Path, sourceMap: Path): CompilationSuccess = {
    val filesWritten = if (Files.exists(sourceMap))
      Set(css, sourceMap)
    else
      Set(css)

    // Extract the file dependencies from the source map.
    Option(compilationResult.sourceMap) match {
      case Some(sourceMapContent) =>
        CompilationSuccess(extractDependencies(css.getParent, sourceMapContent).filter(Files.exists(_)).toSet, filesWritten)
      case None =>
        CompilationSuccess(Set(sass.normalize), filesWritten)
    }
  }

  private def extractDependencies(baseDir: Path, originalSourceMap: String): Seq[Path] =
    normalizeFiles(baseDir, (Json.parse(originalSourceMap) \ "sources")
      .as[Seq[String]])

  private def fixSourceMap(originalSourceMap: String, compilerSettings: CompilerSettings, baseDir: Path): String = {
    val parsedSourceMap = Json.parse(originalSourceMap).as[JsObject]
    // Combine source file references with their contents
    val sources = normalizeFiles(baseDir, (parsedSourceMap \ "sources").as[Seq[String]])
    val sourcesWithContents = sources
      .zip((parsedSourceMap \ "sourcesContent").toOption.map(_.as[Seq[String]]).getOrElse(Stream.continually("")))
      .toMap
      .filterKeys(Files.exists(_)) // Filter non-existing sources

    // Exclude unknown files from the mappings
    val excludedSources = sources.zipWithIndex.toMap.filterKeys(!Files.exists(_))
    val mappings = (parsedSourceMap \ "mappings").toOption.map(_.as[String]).getOrElse("")
    val mappingsWithoutExcludedSources = excludeMappings(SourceMapping.decode(mappings), excludedSources.values.toSet)

    // Use relative file names to make sure that the browser can find the files when they are moved to the target dir
    val transformedSources = sourcesWithContents.keys
        .map(convertToRelativePath(_, compilerSettings.includePaths).toString)
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

  private def normalizeFiles(baseDir: Path, fileNames: Iterable[String]): Seq[Path] =
    fileNames
      .map(f => baseDir.resolve(f).normalize)
      .toSeq

  private def convertToRelativePath(file: Path, includePaths: Iterable[Path]): Path = {
    val normalizedPath = file.normalize
    val ancestorDir = includePaths.find(includePath => normalizedPath.startsWith(includePath))

    ancestorDir match {
      case None => normalizedPath
      case Some(ancestor) => ancestor.relativize(normalizedPath)
    }
  }
}