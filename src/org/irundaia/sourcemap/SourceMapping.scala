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

package org.irundaia.sourcemap

import org.irundaia.base64.Base64VLQ

case class SourceMapping(sourceFileIndex: Int, sourceLine: Int, sourceColumn: Int, targetLine: Int, targetColumn: Int)

object SourceMapping {
  private val lineSeparator = ";"
  private val segmentSeparator = ","

  def decode(input: String): Seq[SourceMapping] = {
    input.split(lineSeparator).toSeq.zipWithIndex.map(decodeLine).reduce(_ ++ _)
  }

  private def doDecodeLine(line: String, targetLine: Int): Seq[SourceMapping] = {
    if (line.isEmpty)
      Seq()
    else {
      val segments = line.split(segmentSeparator).toSeq.map(decodeSegment)
      segments.foldLeft(Seq[SourceMapping]()) {
        case
          (mappings, (targetColumnIncrement, sourceFileIncrement, sourceLineIncrement, sourceColumnIncrement)) =>
            val prevMapping = mappings.headOption.getOrElse(SourceMapping(0, 0, 0, targetLine, 0))
            val nextTargetColumn = prevMapping.targetColumn + targetColumnIncrement
            val nextSourceIndex = prevMapping.sourceFileIndex + sourceFileIncrement
            val nextSourceLine = prevMapping.sourceLine + sourceLineIncrement
            val nextSourceColumn = prevMapping.sourceColumn + sourceColumnIncrement

            SourceMapping(nextSourceIndex, nextSourceLine, nextSourceColumn, targetLine, nextTargetColumn) +: mappings
      }.reverse
    }
  }
  private def decodeLine = (doDecodeLine _).tupled

  private def decodeSegment(segment: String): (Int, Int, Int, Int) = {
    val foundValues = Base64VLQ.decode(segment)

    (foundValues(0), foundValues(1), foundValues(2), foundValues(3))
  }

  def encode(mappings: Seq[SourceMapping]): String = mappings.groupBy(_.targetLine).mapValues(encodeLine).values.mkString(lineSeparator)

  private def encodeLine(mappings: Seq[SourceMapping]): String =
    mappings.foldLeft((Seq[String](), SourceMapping(0, 0, 0, 0, 0))){
      case ((encodedSegments, previousMapping), nextMapping) => (encodeSegment(nextMapping, previousMapping) +: encodedSegments, nextMapping)
    }._1.reverse.mkString(segmentSeparator)

  private def encodeSegment(currentMapping: SourceMapping, previousMapping: SourceMapping): String = {
    val toBeEncodedValues = Seq(
      currentMapping.targetColumn - previousMapping.targetColumn, // Target column increment
      currentMapping.sourceFileIndex - previousMapping.sourceFileIndex, // Source file increment
      currentMapping.sourceLine - previousMapping.sourceLine, // Source line increment
      currentMapping.sourceColumn - previousMapping.sourceColumn // Source column increment
    )

    toBeEncodedValues.map(Base64VLQ.encode).mkString("")
  }
}