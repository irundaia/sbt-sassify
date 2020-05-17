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

package org.irundaia.sass.jna;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

public class SizeT extends IntegerType {
  private static final long serialVersionUID = 1L;

  /** Size of a size_t integer, in bytes. */
  public static int SIZE = Native.SIZE_T_SIZE;

  /** Create a zero-valued Size. */
  public SizeT() {
    this(0);
  }

  /** Create a Size with the given value. */
  public SizeT(long value) {
    super(SIZE, value);
  }
}
