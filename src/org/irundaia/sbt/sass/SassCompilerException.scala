package org.irundaia.sbt.sass

/**
 * An exception that is used to provide error messages from the
 * sass compiler.
 *
 * @param stderr A seq holding error messages.
 */
class SassCompilerException(stderr: Seq[String]) extends RuntimeException {
  /**
   * Combines the error message lines with the system line separator.
   *
   * @return A string holding all error lines.
   */
  override def getMessage: String = stderr.mkString(System.getProperty("line.separator"))
}
