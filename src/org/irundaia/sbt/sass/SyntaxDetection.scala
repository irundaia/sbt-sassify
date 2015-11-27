package org.irundaia.sbt.sass

sealed trait SyntaxDetection
case object Auto extends SyntaxDetection
case object ForceScss extends SyntaxDetection
case object ForceSass extends SyntaxDetection