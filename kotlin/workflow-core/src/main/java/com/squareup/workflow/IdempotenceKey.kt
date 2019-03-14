/*
 * Copyright 2019 Square Inc.
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

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier

/**
 * Helper methods to create opaque idempotence keys for [WorkflowContext] methods.
 *
 * @see fromGenericType
 */
object IdempotenceKey {

  /**
   * Creates an idempotence key based on a raw generic type and its type arguments.
   *
   * E.g. if you need to create an idempotence key for a `ReceiveChannel<String>`, you'd write:
   * ```
   * fromGenericType(ReceiveChannel::class, String::class)
   * ```
   *
   * @param key A String value that can be used to further distinguish when the types are the same.
   */
  fun fromGenericType(
    type: KClassifier,
    vararg typeArgs: KClass<*>,
    key: String
  ): Any = GenericTypeIdempotenceKey(type, typeArgs.asList(), key)

  private data class GenericTypeIdempotenceKey(
    val rawType: KClassifier,
    val typeArgs: List<KClassifier>,
    val key: String
  )
}
