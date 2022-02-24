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

package org.irundaia.util

import java.nio.file.Path
import scala.io.Source

package object extensions {
  implicit class RichPath (p: Path) {
    def withExtension(extension: String): Path = p.resolveSibling(p.getFileName.toString.replaceAll("""(.*)\.\w+""", s"$$1.$extension"))
    def isAncestorOf(other: Path): Boolean = other.toAbsolutePath.toString.startsWith(p.toAbsolutePath.toString)
    def line(line: Int): String = Source.fromFile(p.toString).getLines().drop(line - 1).next
  }
}
