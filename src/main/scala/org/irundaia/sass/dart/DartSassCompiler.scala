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

package org.irundaia.sass.dart

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.irundaia.sass._
import org.irundaia.util.RecursiveFileDeleter
import org.irundaia.util.extensions.RichPath
import sass.embedded_protocol.embedded_sass.InboundMessage.CompileRequest
import sass.embedded_protocol.embedded_sass.InboundMessage.CompileRequest.Importer
import sass.embedded_protocol.embedded_sass.OutboundMessage.CompileResponse.{CompileFailure, CompileSuccess}
import sass.embedded_protocol.embedded_sass.OutboundMessage.{CompileResponse, LogEvent}
import sass.embedded_protocol.embedded_sass._
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.lang.ProcessBuilder.Redirect
import java.net.URI
import java.nio.file._
import scala.collection.compat.toTraversableLikeExtensionMethods
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.util.Try


case class SourceMap(mappings: String, names: Seq[String], sourceRoot: String, sources: Seq[String], version: Int)
case class Success(css: String, sourceMap: SourceMap, loadedUrls: Set[Path], logMessages: Seq[LogMessage])
case class Failure(logMessages: Seq[LogMessage])

case class DartSassCompiler(settings: CompilerSettings) extends SassCompiler {

  import DartSassCompiler._

  //noinspection ScalaStyle
  def determinePlatformPrefix: String = (System.getProperty("os.name").toLowerCase, System.getProperty("os.arch")) match {
    case (os, _) if os.contains("mac") => "darwin"
    case (os, arch) if os.contains("windows") && arch.equals("x86") => "win32-x86"
    case (os, _) if os.contains("windows") => "win32-x86-64"
    case (_, arch) if arch.equals("x86") => "linux-x86"
    case _ => "linux-x86-64"
  }

  def determinePlatformResources: Seq[String] = System.getProperty("os.name").toLowerCase match {
    case os if os.contains("mac") => Seq(
      "dart-sass-embedded",
      "src/dart",
      "src/dart-sass-embedded.snapshot",
      "LICENSE"
    )
    case os if os.contains("windows") => Seq(
      "dart-sass-embedded.bat",
      "src/dart.exe",
      "src/dart-sass-embedded.snapshot",
      "LICENSE"
    )
    case _ => Seq(
      "dart-sass-embedded",
      "LICENSE"
    )
  }

  def extractCompiler(): Path = {
    val prefix = determinePlatformPrefix
    val tempDirPrefix = "dart-sass-compiler"
    val tempDir = settings.workingDir
      .map(p => Files.createTempDirectory(p, tempDirPrefix))
      .getOrElse(Files.createTempDirectory(tempDirPrefix))

    val resources = determinePlatformResources.map(resource => {
      val prefixed = s"$prefix/sass_embedded/$resource"
      (DartSassCompiler.getClass.getResourceAsStream(s"/$prefixed"), tempDir.resolve(resource))
    })
      .filter { case (stream, _) => stream != null }

    // Extract resources from the JAR file
    resources
      .foreach { case (stream, target) =>
        Files.createDirectories(target.getParent)
        Files.copy(stream, target)
      }

    // Make sure that the binaries are executable
    resources
      .map(_._2)
      .filter(resource => !resource.toString.endsWith("snapshot") || !resource.toString.endsWith("LICENSE"))
      .foreach(resource => resource.toFile.setExecutable(true, true))

    tempDir.resolve("dart-sass-embedded")
  }

  private def startCompiler(compilerPath: Path, sourceDir: Path): Process =
    new ProcessBuilder()
      .directory(sourceDir.toFile)
      .redirectError(Redirect.PIPE)
      .command(compilerPath.toString)
      .start()

  private def stopCompiler(process: Process): Unit =
    process.destroy()

  private def modifySourceMap(sourceMap: SourceMap, inputPaths: Seq[Path]): SourceMap = {
    val sortedInputPaths = inputPaths
      .map(_.toAbsolutePath)
      .sortBy(-_.toString.length)

    val relativised = sourceMap
      .sources
      .map(URI.create)
      .map(Paths.get)
      .map(_.toAbsolutePath)
      .map(s => sortedInputPaths
        .find(_.isAncestorOf(s))
        .map(_.relativize(s))
        .getOrElse(s)
      )
      .map(_.toString)
    sourceMap.copy(sourceRoot = settings.sourceMapRoot, sources = relativised)
  }

  override def compile(inputs: Seq[Path],
                       sourceDir: Path,
                       targetDir: Path): Map[Path, Either[CompilationFailure, CompilationSuccess]] = {
    val config = ConfigFactory.defaultReference(ActorSystem.getClass.getClassLoader)
    implicit val actorSystem: ActorSystem = ActorSystem("sass", config, ActorSystem.getClass.getClassLoader)

    val compilerPath = extractCompiler()
    val compilerProcess = startCompiler(compilerPath, sourceDir)

    val result = Try(Await.result(compile(compilerProcess, inputs, sourceDir), 10.seconds))

    actorSystem.terminate()
    stopCompiler(compilerProcess)

    Files.walkFileTree(compilerPath.getParent, new RecursiveFileDeleter())

    val results = result.get
    val fileWriter = outputFiles(sourceDir, targetDir) _
    val writtenFiles = results
      .collect { case (p, Right(success)) => (p, success) }
      .map { case (p, success) => (p, fileWriter(p, success)) }

    results
      .map { case (source, result) =>
        (source, result.fold(
          failure => Left(CompilationFailure(failure.logMessages)),
          success => Right(CompilationSuccess(
            success.loadedUrls,
            writtenFiles.getOrElse(source, Set.empty),
            success.logMessages
          ))
        ))
      }
  }

  private def compile(compilerProcess: Process, inputs: Seq[Path], sourceDir: Path)
                     (implicit materializer: Materializer): Future[Map[Path, Either[Failure, Success]]] = {
    val compileRequests = inputs.zipWithIndex
      .map { case (path, index) => createCompileRequest(index, path, sourceDir) }
      .map(InboundMessage().withCompileRequest)
      .to(collection.immutable.Seq.canBuildFrom)

    doCompile(compilerProcess, inputs, compileRequests)
  }

  private def doCompile(compilerProcess: Process, inputs: Seq[Path], compileRequests: collection.immutable.Seq[InboundMessage])
                       (implicit materializer: Materializer): Future[Map[Path, Either[Failure, Success]]] = {
    val compilationDonePromises: Seq[Promise[Unit]] = inputs.map(_ => Promise[Unit]())

    val (queue, completion) = Source.queue[InboundMessage](10 * inputs.length, OverflowStrategy.backpressure)
      .via(compiler(compilerProcess))
      .map(_.message)
      .wireTap(message =>
        message.compileResponse.foreach(response => compilationDonePromises(response.id).trySuccess(())))
      .collect {
        case message@OutboundMessage.Message.CompileResponse(response) => (response.id, message)
        case message@OutboundMessage.Message.LogEvent(event) => (event.compilationId, message)
      }
      .toMat(Sink.seq)(Keep.both)
      .run()

    compileRequests.foreach(queue.offer)

    Future.sequence(compilationDonePromises.map(_.future))
      .foreach(_ => queue.complete())

    completion
      .map(messages =>
        messages.groupMap { case (id, _) => inputs(id) }(_._2)
          .mapValues(extractCompilationResult)
      )
  }

  private def extractCompilationResult(messages: Seq[OutboundMessage.Message]): Either[Failure, Success] = {
    val logEvents: Seq[LogMessage] = messages
      .flatMap(_.logEvent)
      .map(_.toLogMessage)

    messages.find(_.isCompileResponse).flatMap(_.compileResponse) match {
      case Some(CompileResponse(_, result, _)) =>
        result.value match {
          case CompileSuccess(css, sourceMapString, loadedUrls, _) =>
            Right(Success(
              css,
              sourceMapString.parseJson.convertTo[SourceMap],
              loadedUrls.map(URI.create).map(Paths.get).toSet,
              logEvents
            ))
          case failure: CompileFailure =>
            Left(Failure(failure.toLogMessage +: logEvents))
        }
      case None => Left(Failure(logEvents))
    }
  }

  private def outputFiles(sourceDir: Path, targetDir: Path)(source: Path, success: Success): Set[Path] = {
    val (cssTargetPath, sourceMapTargetPath) =
      determineOutputPaths(sourceDir, targetDir, source, settings.extension)

    val completeCss =
      if (settings.generateSourceMaps)
        s"""
           |${success.css}
           |/*# sourceMappingURL=${targetDir.relativize(sourceMapTargetPath)} */
           |""".stripMargin
      else
        success.css

    val modifiedSourceMap = modifySourceMap(success.sourceMap, sourceDir +: settings.includePaths)
    ensurePathExists(cssTargetPath.getParent)

    outputCss(completeCss, cssTargetPath)

    if (settings.generateSourceMaps) {
      outputSourceMap(modifiedSourceMap.toJson.compactPrint, sourceMapTargetPath)
      Set(cssTargetPath, sourceMapTargetPath)
    } else {
      Set(cssTargetPath)
    }
  }

  private def createCompileRequest(id: Int, sassFile: Path, sourceDir: Path): CompileRequest =
    CompileRequest()
      .withId(id)
      .withPath(sourceDir.relativize(sassFile).toString)
      .withSourceMap(true)
      .withStyle(settings.outputStyle match {
        case Minified => OutputStyle.COMPRESSED
        case _ => OutputStyle.EXPANDED
      })
      .withImporters((sourceDir +: settings.includePaths).map(p => Importer.of(Importer.Importer.Path(p.toString))))
}

object DartSassCompiler {
  implicit class RichSourceSpan(val sourceSpan: SourceSpan) {
    def toLocation: Location = {
      val start = sourceSpan.start.get
      Location(Paths.get(URI.create(sourceSpan.url)), start.line + 1, start.column)
    }
  }

  implicit class RichLogEvent(val event: LogEvent) {
    val severity: Severity = event.`type` match {
      case LogEventType.DEBUG => Severity.Debug
      case LogEventType.WARNING => Severity.Warning
      case LogEventType.DEPRECATION_WARNING => Severity.Warning
      case _ => Severity.Warning
    }

    def toLogMessage: LogMessage = LogMessage(severity, event.message, event.span.map(_.toLocation))
  }

  implicit class RichCompilationFailure(val failure: CompileFailure) {
    def toLogMessage: LogMessage = LogMessage(Severity.Error, failure.message, failure.span.map(_.toLocation))
  }

  implicit val sourceMapFormat: RootJsonFormat[SourceMap] = jsonFormat5(SourceMap)

  def compiler(compilerProcess: Process): Flow[InboundMessage, OutboundMessage, _] =
    Flow.fromFunction[InboundMessage, Array[Byte]](_.toByteArray)
      .map(ByteString.apply)
      .via(VarintFraming.protocol().join(Flow.fromSinkAndSource(
        StreamConverters.fromOutputStream(() => compilerProcess.getOutputStream, autoFlush = true),
        StreamConverters.fromInputStream(() => compilerProcess.getInputStream)
      )))
      .map(_.toArray)
      .map(OutboundMessage.parseFrom)
}
