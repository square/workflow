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

@Suppress("unused")
@Deprecated(
    "Renamed to ViewEnvironment.",
    replaceWith = ReplaceWith("ViewEnvironment", "com.squareup.workflow.ui.ViewEnvironment")
)
typealias ContainerHints = ViewEnvironment

/**
 * Immutable, append-only map of values that a parent view can pass down to
 * its children via [View.showRendering][android.view.View.showRendering] et al.
 * Allows container views to give descendants information about the context in which
 * they're drawing.
 *
 * Every [ViewEnvironment] includes a [ViewRegistry]. This allows container views to
 * make recursive [ViewRegistry.buildView] calls to build child views to show nested renderings.
 */
class ViewEnvironment private constructor(
  private val map: Map<ViewEnvironmentKey<*>, Any>
) {
  constructor(registry: ViewRegistry) :
      this(mapOf<ViewEnvironmentKey<*>, Any>(ViewRegistry to registry))

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(key: ViewEnvironmentKey<T>): T = map[key] as? T ?: key.default

  operator fun <T : Any> plus(pair: Pair<ViewEnvironmentKey<T>, T>): ViewEnvironment =
    ViewEnvironment(map + pair)

  operator fun plus(other: ViewEnvironment): ViewEnvironment = ViewEnvironment(map + other.map)

  override fun toString() = "ViewEnvironment($map)"

  override fun equals(other: Any?) = (other as? ViewEnvironment)?.let { it.map == map } ?: false

  override fun hashCode() = map.hashCode()
}

/**
 * Defines a value that can be provided by a [ViewEnvironment] map, specifying its [type]
 * and [default] value.
 */
abstract class ViewEnvironmentKey<T : Any>(
  private val type: KClass<T>
) {
  abstract val default: T

  final override fun equals(other: Any?) = when {
    this === other -> true
    other != null && this::class != other::class -> false
    else -> type == (other as ViewEnvironmentKey<*>).type
  }

  final override fun hashCode() = type.hashCode()

  override fun toString(): String {
    return "ViewEnvironmentKey($type)-${super.toString()}"
  }
}
