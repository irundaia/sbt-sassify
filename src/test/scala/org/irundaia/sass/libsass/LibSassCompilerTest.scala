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

package org.irundaia.sass.libsass

import org.irundaia.sass.{Auto, CompilerSettings, Minified}
import org.scalatest.funspec.AnyFunSpec;
import org.scalatest.matchers.must._;

import java.nio.file.{Files, Paths}
import scala.io.Source

class LibSassCompilerTest extends AnyFunSpec with Matchers {
  val testDir = Files.createTempDirectory("sbt-sassify")
  val compilerSettings = CompilerSettings(Minified, true, true, Auto, Seq(), "", 10, "css", None)

  describe("The LibSassCompiler") {
    describe("using well formed scss input") {
      describe("without includes") {
        val input = Paths.get(getClass.getResource("/org/irundaia/sass/well-formed.scss").toURI)
        val compilationResults = LibSassCompiler(compilerSettings).compile(input, input.getParent, testDir)

        it("should compile") {
          compilationResults.isRight mustBe true
        }

        it("should contain the proper contents") {
          val cssMin = Source.fromFile(compilationResults.right.get.filesWritten.filter(_.toString.endsWith("css")).head.toFile).mkString
          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read one file") {
          compilationResults.right.get.filesRead.size must be(1)
        }

        it("should have read the correct file") {
          compilationResults.right.get.filesRead.head.toString must endWith("well-formed.scss")
        }
      }

      describe("with includes") {
        val input = Paths.get(getClass.getResource("/org/irundaia/sass/well-formed-using-import.scss").toURI)
        val compilationResults = LibSassCompiler(compilerSettings).compile(input, input.getParent, testDir)

        it("should compile") {
          compilationResults.isRight mustBe true
        }

        it("should include the contents of both the included and the including file") {
          val cssMin = Source.fromFile(compilationResults.right.get.filesWritten.filter(_.toString.endsWith("css")).head.toFile).mkString
          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

          testMinCss must include(".test-import{font-weight:bold")
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read two files") {
          compilationResults.right.get.filesRead.size must be(2)
        }

        it("should have read the included file") {
          compilationResults.right.get.filesRead.filter(_.endsWith("_well-formed-import.scss")) must not be None
        }
      }
    }

    describe("using broken scss input") {
      val input = Paths.get(getClass.getResource("/org/irundaia/sass/broken-input.scss").toURI)
      val compilationResult = LibSassCompiler(compilerSettings).compile(input, input.getParent, testDir)

      describe("should fail compilation") {
        compilationResult.isLeft mustBe true
      }

      describe("should throw an exception") {
        it("report an error message") {
          compilationResult match {
            case Left(exception) => exception.logMessages.head.message must not be empty
            case _ => fail
          }
        }

        it("reporting an error on line 2 column 16") {
          compilationResult match {
            case Left(failure) =>
              val message = failure.logMessages.head
              message.location.get.line mustBe 2
              message.location.get.column mustBe 16
            case _ => fail
          }
        }
      }
    }
  }
}
