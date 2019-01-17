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
package com.squareup.viewregistry

/**
 * A collection of [ViewBinding]s, keyed to the names of the screen types
 * they render.
 */
class ViewRegistry private constructor(
  private val bindings: Map<String, ViewBinding<*>>
) {
  constructor(vararg bindings: ViewBinding<*>) : this(
      bindings.map { it.type to it }.toMap().apply {
        check(keys.size == bindings.size) {
          "${bindings.map { it.type }} should not have duplicate entries."
        }
      }
  )

  constructor(vararg registries: ViewRegistry) : this(registries.map { it.bindings }
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
  operator fun <T : Any> get(type: String): ViewBinding<T> {
    require(type in bindings) { "Unrecognized screen type $type" }

    @Suppress("UNCHECKED_CAST")
    return bindings[type] as ViewBinding<T>
  }

  operator fun <T : Any> plus(binding: ViewBinding<T>): ViewRegistry {
    check(binding.type !in bindings.keys) {
      "Already registered ${bindings[binding.type]} for ${binding.type}, cannot accept $binding."
    }

    return ViewRegistry(bindings + (binding.type to binding))
  }

  operator fun plus(registry: ViewRegistry): ViewRegistry {
    return ViewRegistry(this, registry)
  }
}
