/*
 * Copyright 2016 Han van Venrooij
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

import java.io.File

import play.api.libs.json.{JsObject, Json}

import scala.io.Source

sealed trait CompilationFailure {
  def getMessage: String
}
case class LineBasedCompilationFailure(message: String, line: Int, column: Int, lineContent: String, source: File)
  extends CompilationFailure {
  override def getMessage: String =
    s"""Compilation error on line $line of $source:
        |$lineContent
        |${" " * column}^
        |$message""".stripMargin
}
case class GenericCompilationFailure(message: String) extends CompilationFailure {
  override def getMessage: String =
    s"""Compilation error:
       |$message
     """.stripMargin
}

object CompilationFailure {
  def apply(compilationOutput: Output): CompilationFailure = {
    if (compilationOutput.errorStatus == 1) applyLineBased(compilationOutput) else applyGeneric(compilationOutput)
  }

  private def applyLineBased(compilationOutput: Output) = {
    val errorJson = Json.parse(compilationOutput.errorJson).as[JsObject]

    val message: String = compilationOutput.errorMessage
    val line = (errorJson \ "line").as[Int]
    val column = (errorJson \ "column").as[Int]
    val source = new File(compilationOutput.errorFile)
    val lineContent = Source.fromFile(source).getLines().drop(line - 1).next

    new LineBasedCompilationFailure(message, line, column - 1, lineContent, source)
  }
  private def applyGeneric(compilationOutput: Output) = {
    new GenericCompilationFailure(compilationOutput.errorMessage)
  }
}