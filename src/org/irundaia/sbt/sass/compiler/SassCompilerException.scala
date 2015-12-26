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
import xsbti.{Problem, Severity}

import scala.io.Source

sealed trait SassCompilerException extends RuntimeException
case class SassCompilerLineBasedException(message: String, line: Int, column: Int, lineContent: String, source: File)
  extends SassCompilerException {
  override def getMessage: String =
    s"""Compilation error on line $line of $source:
        |$lineContent
        |${" " * column}^
        |$message""".stripMargin
}
case class SassCompilerGenericException(message: String) extends SassCompilerException {
  override def getMessage: String =
    s"""Compilation error:
       |$message
     """.stripMargin
}

object SassCompilerException {
  def apply(compilationOutput: Output): SassCompilerException = {
    if (compilationOutput.getErrorStatus == 1) applyLineBased(compilationOutput) else applyGeneric(compilationOutput)
  }

  private def applyLineBased(compilationOutput: Output) = {
    val errorJson = Json.parse(compilationOutput.getErrorJson).as[JsObject]

    val message: String = compilationOutput.getErrorMessage
    val line = (errorJson \ "line").as[Int]
    val column = (errorJson \ "column").as[Int]
    val source = new File(compilationOutput.getErrorFile)
    val lineContent = compilationOutput.getErrorSrc.split("\n").drop(line - 1).head

    new SassCompilerLineBasedException(message, line, column - 1, lineContent, source)
  }
  private def applyGeneric(compilationOutput: Output) = {
    new SassCompilerGenericException(compilationOutput.getErrorMessage)
  }
}