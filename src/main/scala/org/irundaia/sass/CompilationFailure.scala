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

import java.io.File

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
  def apply(error: SassError): CompilationFailure = {
    if (error.status == 1) applyLineBased(error) else applyGeneric(error)
  }

  private def applyLineBased(error: SassError) = {
    val message: String = error.message
    val source = new File(error.file)
    val line = error.line
    val column = error.column
    val lineContent = Source.fromFile(error.file).getLines().drop(line - 1).next

    LineBasedCompilationFailure(message, line, column - 1, lineContent, source)
  }
  private def applyGeneric(error: SassError) = {
    GenericCompilationFailure(error.message)
  }
}
