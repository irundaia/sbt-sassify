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

import java.nio.file.Path

import org.irundaia.sass.jna.SassLibrary

case class Context(nativeContext: SassLibrary.Sass_File_Context) {

  def options: Options = Options(this)

  def cleanup(): Unit = {
    SassCompiler.libraryInstance.sass_delete_file_context(nativeContext)
  }
}

object Context {
  def apply(file: Path): Context = Context(SassCompiler.libraryInstance.sass_make_file_context(file.toFile.getAbsolutePath))
}
