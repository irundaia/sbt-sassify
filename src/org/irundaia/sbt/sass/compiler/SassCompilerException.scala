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

import com.typesafe.sbt.web.LineBasedProblem
import io.bit3.jsass.Output
import play.api.libs.json.{JsObject, Json}
import xsbti.Severity

import scala.io.Source

class SassCompilerException(val message: String, line: Int, column: Int, lineContent: String, source: File)
  extends RuntimeException {
  def problem = new LineBasedProblem(message, Severity.Error, line, column, lineContent, source)

  override def getMessage: String =
    s"""Compilation error on line $line of $source:
        |$lineContent
        |${" " * (column - 1)}^
        |$message
     """.stripMargin
}

object SassCompilerException {
  def apply(compilationOutput: Output) = {
    val errorJson = Json.parse(compilationOutput.getErrorJson).as[JsObject]

    val message: String = (errorJson \ "message").as[String]
    val line = (errorJson \ "line").as[Int]
    val column = (errorJson \ "column").as[Int]
    val source = new File((errorJson \ "file").as[String])

    val lineContent = Source.fromFile(source).getLines().drop(line - 1).next

    new SassCompilerException(message, line, column, lineContent, source)
  }
}
