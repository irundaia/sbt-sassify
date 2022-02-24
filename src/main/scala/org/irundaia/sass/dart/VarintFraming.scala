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

package org.irundaia.sass.dart

import akka.NotUsed
import akka.stream.scaladsl.{BidiFlow, Flow, Keep}
import akka.util.{ByteString, ByteStringBuilder}

import scala.annotation.tailrec
import scala.collection.immutable.Seq

object VarintFraming {
  implicit class RichByteString(bs: ByteString) {
    // TODO DELETE!
    //    def prettyPrint(): String = bs
    //      .map(b => String.format("%8s", (b & 0xFF).toBinaryString)
    //        .replaceAll(" ", "0"))
    //      .mkString("")
    //      .grouped(4)
    //      .mkString(" ")
  }

  def protocol(): BidiFlow[ByteString, ByteString, ByteString, ByteString, NotUsed] =
    BidiFlow.fromFlowsMat(
      Flow[ByteString].map(bs => encode(bs.length) ++ bs),
      Flow[ByteString].statefulMapConcat[ByteString](() => {
        var buffer: ByteString = ByteString.empty

        @tailrec
        def tryDecode(acc: Seq[ByteString] = Seq.empty[ByteString]): Seq[ByteString] = {
          decode(buffer) match {
            case Some((readBytes, size)) if buffer.size >= readBytes + size =>
              val bytes = buffer.slice(readBytes, readBytes + size).compact
              buffer = buffer.drop(readBytes + size)
              tryDecode(bytes +: acc)
            case _ =>
              acc
          }
        }

        bytes => {
          buffer = buffer ++ bytes
          tryDecode()
        }
      })
    )(Keep.left)

  private val mostSignificantBit = 0x80
  private val bitsPerByte = 7
  private val leastSignificantBits = 0x7F

  @tailrec
  def encode(size: Int, builder: ByteStringBuilder = ByteString.createBuilder): ByteString = {
    val bits = size & leastSignificantBits
    val remainder = size >>> bitsPerByte

    if (remainder == 0) {
      builder.putByte(bits.toByte)
      builder.result()
    } else {
      builder.putByte((bits | mostSignificantBit).toByte)
      encode(remainder, builder)
    }
  }

  def decode(byteString: ByteString): Option[(Int, Int)] = {
    val closingByte = byteString.indexWhere(b => (b & mostSignificantBit) == 0)
    Option(closingByte + 1)
      .filter(_ > 0)
      .map(byteCount =>
        (byteCount, byteString.take(byteCount)
          .take(closingByte + 1)
          .map(b => b & leastSignificantBits)
          .zipWithIndex
          .map { case (b, index) => b << (bitsPerByte * index) }
          .sum
        )
      )
  }
}
