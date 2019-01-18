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

import android.content.Context
import android.transition.Scene
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable

/**
 * Factory for [View] or [Scene] instances that can render a stream of screens
 * of the specified [type][T]. Use [LayoutBinding.of] to work with XML layout
 * resources, or [BuilderBinding] to create views from code.
 *
 * Sets of bindings are gathered in [ViewRegistry] instances.
 */
interface ViewBinding<T : Any> {
  // Tried making this Class<T>, but got into trouble w/type invariance.
  // https://github.com/square/workflow/issues/18
  val type: String

  fun buildView(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup? = null
  ): View

  fun buildScene(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup,
    enterAction: ((Scene) -> Unit)? = null
  ): Scene
}

fun <T : Any> ViewBinding<T>.buildView(
  screens: Observable<out T>,
  viewRegistry: ViewRegistry,
  container: ViewGroup
): View = buildView(screens, viewRegistry, container.context, container)

fun <T : Any> ViewBinding<T>.buildScene(
  screens: Observable<out T>,
  viewRegistry: ViewRegistry,
  container: ViewGroup,
  enterAction: ((Scene) -> Unit)? = null
): Scene = buildScene(screens, viewRegistry, container.context, container, enterAction)

fun Scene.viewOrNull(): View? {
  return if (sceneRoot.childCount > 0) {
    sceneRoot.getChildAt(0)
  } else {
    null
  }
}
