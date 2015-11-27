package org.irundaia.sbt.sass

import java.io.{File, FileWriter}

import com.typesafe.sbt.web.incremental.OpSuccess
import io.bit3.jsass.{Compiler, Options, OutputStyle}
import java.util.regex.Pattern
import play.api.libs.json._

class SassCompiler(compilerSettings: CompilerSettings) {

  def compile(source: File, baseDirectory: File, sourceDir: File, targetDir: File): OpSuccess = {
    // Determine the source filename (relative to the source directory)
    val fileName = source.getPath.replaceAll(Pattern.quote(sourceDir.getPath), "").replaceFirst("""\.\w+""", "")
    def sourceWithExtn(extn: String): File = new File(s"$targetDir$fileName.$extn")

    // Determine target files
    val targetCss = sourceWithExtn("css")
    val targetCssMap = sourceWithExtn("css.map")

    // Make sure that the target directory is created
    targetCss.getParentFile.mkdirs()

    // Compile the sources, and get the dependencies
    val dependencies =
      doCompile(
        source,
        targetCss,
        targetCssMap,
        baseDirectory.getAbsolutePath,
        source.getParent
      )

    val readFiles = dependencies.map(new File(_)) + source

    // Return which files have been read/written
    OpSuccess(readFiles, Set(targetCss, targetCssMap))
  }


  def doCompile(in: File, out: File, map: File, baseDir: String, sourceDir: String): Set[String] = {
    // Set compiler options
    val options = generateOptions(in, map)

    // Compile to CSS
    val compiler = new Compiler
    val compiled = compiler.compileFile(in.toURI, out.toURI, options)

    // Output the CSS or throw an exception when compilation failed
    Option(compiled.getCss) match {
      case Some(css) =>
        val cssWriter = new FileWriter(out)

        cssWriter.write(css)
        cssWriter.close()
      case _ => throw new SassCompilerException(compiled.getErrorMessage.split(System.lineSeparator))
    }

    // Output the source map in case it should be output
    Option(compiled.getSourceMap) match {
      case Some(sourceMap) =>
        val (revisedMap, dependencies) = extractDependencies(sourceMap, baseDir, sourceDir)

        val mapWriter = new FileWriter(map)

        mapWriter.write(revisedMap)
        mapWriter.close()
      case _ => // Do not output any source map
    }

    // Extract the file dependencies from the source map.
    Option(compiled.getSourceMap) match {
      case Some(sourceMap) =>
        extractDependencies(sourceMap, baseDir, sourceDir)._2
      case None =>
        Set[String]()
    }
  }

  private def generateOptions(sourceFile: File, sourceMapFile: File): Options = {
    val compilerStyle = compilerSettings.style match {
      case Minified => OutputStyle.COMPRESSED
      case Maxified => OutputStyle.EXPANDED
      case Sassy => OutputStyle.NESTED
    }

    val options = new Options
    options.setSourceMapFile(sourceMapFile.toURI)
    // Note the source map will always be generated to determine the parsed files
    options.setOmitSourceMapUrl(!compilerSettings.generateSourceMaps)
    options.setSourceMapContents(false)
    options.setOutputStyle(compilerStyle)

    // Determine syntax version
    if (sourceFile.getPath.endsWith("sass"))
      options.setIsIndentedSyntaxSrc(true)

    options
  }

  private def extractDependencies(originalMap: String, baseDir: String, sourceDir: String): (String, Set[String]) = {
    val parsed = Json.parse(originalMap)
    val transformedDependencies = (parsed \ "sources").as[Set[String]].map(transformDependency(_, baseDir, sourceDir))
    val transformedMap =
      parsed.as[JsObject] ++ Json.obj("sources" -> JsArray(transformedDependencies.map(JsString.apply).toSeq))

    (Json.stringify(transformedMap), transformedDependencies)
  }

  private def transformDependency(fileName: String, baseDir: String, sourceDir: String): String =
    // transform the dependency string to make sure that browsers can understand it
    (baseDir + File.separator + fileName.replaceAll("""(\.\.\/)|(\.\.\\)""", ""))
	  .replaceFirst(Pattern.quote(sourceDir + File.separator), "")
}
