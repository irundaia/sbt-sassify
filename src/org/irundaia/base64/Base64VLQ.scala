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

package org.irundaia.base64

import scala.annotation.tailrec

object Base64VLQ {

  private val baseShift = 5
  private val base = 1 << baseShift
  private val baseMask = base - 1
  private val continuationBit = base
  private val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
  private val base64CharMap = base64Chars.zipWithIndex.toMap

  def decode(encoded: String): Seq[Int] = {
    val digits = encoded.map(base64CharMap)

    digits.foldLeft((Seq[Int](), 0, 0)){
      case ((values, currentValue, shift), nextDigit) =>
        val resultValue = currentValue + (decodedValue(nextDigit) << shift)

        if (shouldContinueDecode(nextDigit)) {
          (values, resultValue, shift + baseShift)
        } else {
          if (shouldNegateDecodedValue(resultValue)) {
            (values ++ Seq(-(resultValue >> 1)), 0, 0)
          } else {
            (values ++ Seq(resultValue >> 1), 0, 0)
          }
        }
    }._1
  }

  def encode(value: Int): String = {
    val toEncodeValue = if (value < 0)
      ((-value) << 1) | 1
    else
      value << 1

    encodePartial(toEncodeValue)
  }

  @tailrec private def encodePartial(value: Int, acc: String = ""): String = {
    val digit = value & baseMask
    val nextValue = value >>> baseShift
    val base64Char = base64Chars(if (nextValue > 0) digit | continuationBit else digit)

    if (nextValue > 0)
      encodePartial(nextValue, acc + base64Char)
    else
      acc + base64Char
  }

  private def shouldContinueDecode(digit: Int): Boolean = (digit & continuationBit) > 0
  private def shouldNegateDecodedValue(value: Int): Boolean = (value & 1) > 0
  private def decodedValue(digit: Int): Int = digit & baseMask
}