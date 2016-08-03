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

import org.irundaia.sass.jna.SassLibrary

case class Output(
   css: String,
   sourceMap: String,
   readFiles: Array[String],
   errorStatus: Int,
   errorJson: String,
   errorText: String,
   errorMessage: String,
   errorFile: String)

object Output {
  def apply(context: Context): Output = {
    val instance = SassLibrary.INSTANCE
    val nativeContext = instance.sass_file_context_get_context(context.nativeContext)
    Output(
      instance.sass_context_get_output_string(nativeContext),
      instance.sass_context_get_source_map_string(nativeContext),
      instance.sass_context_get_included_files(nativeContext),
      instance.sass_context_get_error_status(nativeContext),
      instance.sass_context_get_error_json(nativeContext),
      instance.sass_context_get_error_text(nativeContext),
      instance.sass_context_get_error_message(nativeContext),
      instance.sass_context_get_error_file(nativeContext)
    )
  }
}
