/*
 * Copyright 2017 Square Inc.
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
package com.squareup.workflow

import okio.BufferedSink
import okio.BufferedSource

// These are defined for JVM-only right now because there's no common code to enumerate enum
// constants.

inline fun <reified T : Enum<T>> BufferedSource.readOptionalEnumByOrdinal(): T? {
  return readNullable { readEnumByOrdinal<T>() }
}

fun <T : Enum<T>> BufferedSink.writeOptionalEnumByOrdinal(enumVal: T?): BufferedSink {
  return writeNullable(enumVal) { writeEnumByOrdinal(it) }
}

inline fun <reified T : Enum<T>> BufferedSource.readEnumByOrdinal(): T {
  return T::class.java.enumConstants[readInt()]
}

fun <T : Enum<T>> BufferedSink.writeEnumByOrdinal(enumVal: T): BufferedSink {
  return writeInt(enumVal.ordinal)
}
