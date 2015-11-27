package org.irundaia.sbt.sass


sealed trait CssStyle
object Minified extends CssStyle
object Maxified extends CssStyle
object Sassy extends CssStyle