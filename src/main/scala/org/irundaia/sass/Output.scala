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

sealed trait Output

case class SassError(
    status: Int,
    text: String,
    message: String,
    file: String,
    line: Int,
    column: Int) extends Output

case class SassOutput(
    css: String,
    sourceMap: String,
    readFiles: Array[String]) extends Output

object Output {
  def apply(context: Context): Output = {
    val instance = SassCompiler.libraryInstance
    val nativeContext = instance.sass_file_context_get_context(context.nativeContext)

    if (instance.sass_context_get_error_status(nativeContext) == 0) {
      SassOutput(
        instance.sass_context_get_output_string(nativeContext).getString(0, "UTF-8"),
        instance.sass_context_get_source_map_string(nativeContext).getString(0, "UTF-8"),
        instance.sass_context_get_included_files(nativeContext)
      )
    } else {
      SassError(
        instance.sass_context_get_error_status(nativeContext),
        instance.sass_context_get_error_text(nativeContext),
        instance.sass_context_get_error_message(nativeContext),
        instance.sass_context_get_error_file(nativeContext),
        instance.sass_context_get_error_line(nativeContext).intValue(),
        instance.sass_context_get_error_column(nativeContext).intValue()
      )
    }
  }
}
