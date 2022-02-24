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

import java.io.IOException
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}
import java.nio.file.attribute.BasicFileAttributes

final class RecursiveFileDeleter extends FileVisitor[Path] {
  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = FileVisitResult.CONTINUE

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    Files.delete(file)
    FileVisitResult.CONTINUE
  }

  override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.TERMINATE

  override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
    Files.delete(dir)
    FileVisitResult.CONTINUE
  }
}
