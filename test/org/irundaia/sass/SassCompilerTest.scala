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

package org.irundaia.sass

import java.io.File
import java.nio.file.{Paths, Files}

import org.scalatest.{FunSpec, MustMatchers}
import play.api.libs.json.Json

import scala.io.Source
import scala.util.Failure

class SassCompilerTest extends FunSpec with MustMatchers {
  val testDir = Files.createTempDirectory("sbt-sassify")
  val compilerSettings = CompilerSettings(Minified, true, true, Auto, Seq(), "")

  describe("The SassCompiler") {
    describe("using well formed scss input") {
      describe("without includes") {
        val input = Paths.get(getClass.getResource("/org/irundaia/sass/well-formed.scss").toURI)
        val compilationResults = SassCompiler.compile(input, input.getParent, testDir, compilerSettings)

        it("should compile") {
          compilationResults.isSuccess mustBe true
        }

        it("should contain the proper contents") {
          val cssMin = Source.fromFile(compilationResults.get.filesWritten.filter(_.toString.endsWith("css")).head.toFile).mkString
          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read one file") {
          compilationResults.get.filesRead.size must be(1)
        }

        it("should have read the correct file") {
          compilationResults.get.filesRead.head.toString must endWith("well-formed.scss")
        }

        it ("the source map should not have an entry in the sourcesContent field referring to jsass") {
          val cssMin = Source.fromFile(compilationResults.get.filesWritten.filter(_.toString.endsWith("map")).head.toFile).getLines().mkString("\n")
          val parsedSourceMap = Json.parse(cssMin)
          val jsassContents = (parsedSourceMap \ "sourcesContent").as[Seq[String]].filter(_.startsWith("$jsass-void"))

          jsassContents mustBe empty
        }
      }

      describe("with includes") {
        val input = Paths.get(getClass.getResource("/org/irundaia/sass/well-formed-using-import.scss").toURI)
        val compilationResults = SassCompiler.compile(input, input.getParent, testDir, compilerSettings)

        it("should compile") {
          compilationResults.isSuccess mustBe true
        }

        it("should include the contents of both the included and the including file") {
          val cssMin = Source.fromFile(compilationResults.get.filesWritten.filter(_.toString.endsWith("css")).head.toFile).mkString
          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

          testMinCss must include(".test-import{font-weight:bold")
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read two files") {
          compilationResults.get.filesRead.size must be(2)
        }

        it("should have read the included file") {
          compilationResults.get.filesRead.filter(_.endsWith("_well-formed-import.scss")) must not be None
        }
      }
    }

    describe("using broken scss input") {
      val input = Paths.get(getClass.getResource("/org/irundaia/sass/broken-input.scss").toURI)
      val compilationResult = SassCompiler.compile(input, input.getParent, testDir, compilerSettings)

      describe("should fail compilation") {
        compilationResult.isFailure mustBe true
      }

      describe("should throw an exception") {
        it("reporting Invalid CSS") {
          compilationResult match {
            case Failure(exception) => exception.getMessage must include("Invalid CSS after ")
            case _ => fail
          }
        }

        it("reporting an error on line 2 column 16") {
          compilationResult match {
            case Failure(exception: LineBasedCompilerException) =>
              exception.line mustBe 2
              exception.column mustBe 16
            case _ => fail
          }
        }
      }
    }
  }
}