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

/**
 * For flows that navigate through a series of main / body screens, sometimes covered by
 * the occasional modal dialog.
 */
typealias StackedMainAndModalScreen<M, D> = MainAndModalScreen<BackStackScreen<M>, D>

/**
 * For flows that show a [main] screen, optionally covered by a number of nested
 * [modals].
 *
 * @param modals A list of modal screens to show over [main]. This is a list to support
 * modeling of things like nested alerts, or wizards over wizards over wizards.
 *
 * @param M the type of the [main] / body screen, typically `StackScreen<*>`.
 * (See [StackedMainAndModalScreen].)
 * @param D type of the [modals] / dialogs
 */
data class MainAndModalScreen<out M : Any, out D : Any>(
  val main: M,
  val modals: List<D> = emptyList()
) {
  constructor(
    main: M,
    modal: D
  ) : this(main, listOf(modal))
}

@Suppress("FunctionName")
fun <M : Any, D : Any> StackedMainAndModalScreen(
  main: M,
  modal: D? = null
): StackedMainAndModalScreen<M, D> {
  return MainAndModalScreen(BackStackScreen(main), modal?.let { listOf(it) } ?: emptyList())
}

fun <M : Any, D : Any> BackStackScreen<M>.toMainAndModal(): StackedMainAndModalScreen<M, D> {
  return MainAndModalScreen(this)
}
