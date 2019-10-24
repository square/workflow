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
package com.squareup.workflow.ui

import kotlin.reflect.KClass

/**
 * Immutable, append-only map of values that are passed down the view tree
 * via [android.view.View.showRendering] et al. Can be used by container views
 * to give descendants information about the context in which they're drawing.
 */
class Hints private constructor(
  private val map: Map<HintKey<*>, Any>
) {
  constructor() : this(emptyMap())

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(key: HintKey<T>): T = map[key] as? T ?: key.default

  operator fun <T : Any> plus(pair: Pair<HintKey<T>, T>): Hints {
    return Hints(map + pair)
  }

  operator fun plus(other: Hints): Hints = Hints(map + other.map)

  override fun toString(): String {
    return "Hints($map)"
  }

  override fun equals(other: Any?) = (other as? Hints)?.let { it.map == map } ?: false

  override fun hashCode() = map.hashCode()
}

/**
 * Defines a value that can be provided by a [Hints] map, specifying its [type]
 * and [default] value.
 */
abstract class HintKey<T : Any>(
  private val type: KClass<T>
) {
  abstract val default: T

  final override fun equals(other: Any?) = when {
    this === other -> true
    other != null && this::class != other::class -> false
    else -> type == (other as HintKey<*>).type
  }

  final override fun hashCode() = type.hashCode()

  override fun toString(): String {
    return "Hint($type)"
  }
}
