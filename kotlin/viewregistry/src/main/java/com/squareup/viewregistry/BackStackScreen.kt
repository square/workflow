/*
 * Copyright 2018 Square Inc.
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

import kotlin.reflect.KClass

/**
 * Wraps screens that may be shown in series, drill down or wizard style.
 * Typically these are the leaves of composite UI structures. That is, it's
 * probably a mistake if you find yourself creating, say, a
 * `BackStackScreen<AlertContainerScreen<*>>`.
 *
 * @throws IllegalArgumentException if [T] is [BackStackScreen]
 */
data class BackStackScreen<out T : Any>(
  val wrapped: T,
  private val keyExtension: String = ""
) {
  init {
    require(wrapped !is BackStackScreen<*>) {
      "Surely you didn't mean to put a stack right in a stack."
    }
  }

  val key = Key(wrapped::class, keyExtension)

  data class Key<T : Any>(
    val type: KClass<T>,
    val extension: String = ""
  )
}
