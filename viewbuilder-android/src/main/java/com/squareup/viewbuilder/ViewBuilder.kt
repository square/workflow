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
package com.squareup.viewbuilder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.squareup.viewbuilder.ViewBuilder.Registry
import io.reactivex.Observable

interface ViewBuilder<T : Any> {
  // Tried making this Class<T>, but got into trouble w/type invariance.
  // https://github.com/square/workflow/issues/18
  val type: String

  fun buildView(
    screens: Observable<out T>,
    builders: Registry,
    contextForNewView: Context,
    container: ViewGroup? = null
  ): View

  class Registry private constructor(
    private val builders: Map<String, ViewBuilder<*>>
  ) {
    constructor(vararg builders: ViewBuilder<*>) : this(
        builders.map { it.type to it }.toMap().apply {
          check(keys.size == builders.size) {
            "${builders.map { it.type }} should not have duplicate entries."
          }
        }
    )

    constructor(vararg registries: Registry) : this(registries.map { it.builders }
        .reduce { left, right ->
          val duplicateKeys = left.keys.intersect(right.keys)
          check(duplicateKeys.isEmpty()) { "Should not have duplicate entries $duplicateKeys." }
          left + right
        })

    // This is why I can't make the type field up there Class<T>. If I change this
    // method to get(type: Class<T>) the return type is coerced to ViewBuilder<out T>,
    // and everything falls apart.
    //
    // https://github.com/square/workflow/issues/18
    operator fun <T : Any> get(type: String): ViewBuilder<T> {
      require(type in builders) { "Unrecognized screen type $type" }

      @Suppress("UNCHECKED_CAST")
      return builders[type] as ViewBuilder<T>
    }

    operator fun <T : Any> plus(builder: ViewBuilder<T>): Registry {
      check(builder.type !in builders.keys) {
        "Already registered ${builders[builder.type]} for ${builder.type}, cannot accept $builder."
      }

      return Registry(builders + (builder.type to builder))
    }

    operator fun plus(registry: Registry): Registry {
      return Registry(this, registry)
    }
  }
}

fun <T : Any> ViewBuilder<T>.buildView(
  screens: Observable<out T>,
  builders: Registry,
  container: ViewGroup
): View = buildView(screens, builders, container.context, container)
