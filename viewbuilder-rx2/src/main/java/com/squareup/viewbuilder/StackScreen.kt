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
package com.squareup.viewbuilder

import io.reactivex.Observable

/**
 * Wraps screens that may be shown in series, drill down or wizard style.
 * Typically these are the leaves of composite UI structures. That is, it's
 * probably a mistake if you find yourself creating, say, a
 * `StackScreen<MainAndModalScreen<*, *>>`.
 */
data class StackScreen<T : Any>(
  val wrapped: T,
  private val keyExtension: String = ""
) {
  val key = ViewStackKey(wrapped::class.java, keyExtension)
}

data class ViewStackKey<T : Any>(
  val type: Class<T>,
  val extension: String
)

fun <T : Any> Observable<out StackScreen<*>>.matchingWrappedScreens(
  screen: StackScreen<T>
): Observable<out T> {
  return filter { it.key == screen.key }.map {
    screen.key.type.cast(it.wrapped)
  }
}
