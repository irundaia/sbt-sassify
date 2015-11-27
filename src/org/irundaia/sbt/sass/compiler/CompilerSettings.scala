package org.irundaia.sbt.sass.compiler

import org.irundaia.sbt.sass.{CssStyle, SyntaxDetection}

case class CompilerSettings(style: CssStyle, generateSourceMaps: Boolean, embedSources: Boolean, syntaxDetection: SyntaxDetection)
