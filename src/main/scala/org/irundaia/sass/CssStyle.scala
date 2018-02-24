/*
 * Copyright 2018 Han van Venrooij
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

import org.irundaia.sass.jna.SassLibrary

sealed trait CssStyle {
  def intValue: Int
}
case object Minified extends CssStyle {
  override def intValue: Int = SassLibrary.Sass_Output_Style.SASS_STYLE_COMPRESSED
}
case object Maxified extends CssStyle {
  override def intValue: Int = SassLibrary.Sass_Output_Style.SASS_STYLE_EXPANDED
}
case object Sassy extends CssStyle {
  override def intValue: Int = SassLibrary.Sass_Output_Style.SASS_STYLE_NESTED
}
