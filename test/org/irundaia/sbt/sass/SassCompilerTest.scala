package org.irundaia.sbt.sass

import java.io.File

import org.scalatest.{FunSpec, MustMatchers}

import scala.io.Source

class SassCompilerTest extends FunSpec with MustMatchers {
  describe("SassCompiler") {
    describe("using well formed scss input") {
      describe("without includes") {
        it("should compile") {
          val input = new File(getClass.getResource("/org/irundaia/sbt/sass/well-formed.scss").toURI)
          val outputMinified = File.createTempFile("sbt-sass-test", ".css")
          val mapMinified = File.createTempFile("sbt-sass-test", ".css.map")

          val processOutput = new SassCompiler(CompilerSettings(Minified, generateSourceMaps = true))
            .doCompile(input, outputMinified, mapMinified, "", "")
          val cssMin = Source.fromFile(outputMinified).mkString

          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")

          processOutput.size must be(1)
          processOutput.head must include("well-formed.scss")
        }
      }

      describe("with includes") {
        it("should compile") {
          val input = new File(getClass.getResource("/org/irundaia/sbt/sass/well-formed-using-import.scss").toURI)
          val outputMinified = File.createTempFile("sbt-sass-test", ".css")
          val mapMinified = File.createTempFile("sbt-sass-test", ".css.map")

          val processOutput = new SassCompiler(CompilerSettings(Minified, generateSourceMaps = true))
            .doCompile(input,  outputMinified, mapMinified, "", "")

          val cssMin = Source.fromFile(outputMinified).mkString

          val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")
          testMinCss must include(".test-import{font-weight:bold")
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")

          processOutput.size must be(2)
          processOutput.find(_.contains("_well-formed-import.scss")) must not be None
        }
      }
    }

    describe("using broken scss input") {
      it("should throw an exception") {
        val input = new File(getClass.getResource("/org/irundaia/sbt/sass/broken-input.scss").toURI)
        val outputMinified = File.createTempFile("sbt-sass-test", ".css")
        val mapMinified = File.createTempFile("sbt-sass-test", ".css.map")

        val exception = the [SassCompilerException] thrownBy
          new SassCompiler(CompilerSettings(Minified, generateSourceMaps = true))
            .doCompile(input, outputMinified, mapMinified, "", "")

        exception.getMessage must include("invalid property name")
      }
    }
  }
}
