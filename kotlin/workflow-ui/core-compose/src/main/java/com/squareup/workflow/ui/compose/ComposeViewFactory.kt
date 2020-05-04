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
// See https://youtrack.jetbrains.com/issue/KT-31734
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.workflow.ui.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.compose.StructurallyEqual
import androidx.compose.mutableStateOf
import androidx.ui.core.setContent
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.bindShowRendering
import kotlin.reflect.KClass

/**
 * Creates a [ViewFactory] that uses a [Composable] function to display the rendering.
 *
 * Note that the function you pass in will not have any `MaterialTheme` applied, so views that rely
 * on Material theme attributes must be explicitly wrapped with `MaterialTheme`.
 *
 * Simple usage:
 *
 * ```
 * // Function references to @Composable functions aren't supported yet.
 * val FooBinding = bindCompose { showFoo(it) }
 *
 * @Composable
 * private fun showFoo(foo: FooRendering) {
 *   MaterialTheme {
 *     Text(foo.message)
 *   }
 * }
 *
 * …
 *
 * val viewRegistry = ViewRegistry(FooBinding, …)
 * ```
 */
inline fun <reified RenderingT : Any> bindCompose(
  noinline showRendering: @Composable() (RenderingT, ViewEnvironment) -> Unit
): ViewFactory<RenderingT> = ComposeViewFactory(RenderingT::class) { rendering, environment ->
  showRendering(rendering, environment)
}

@PublishedApi
internal class ComposeViewFactory<RenderingT : Any>(
  override val type: KClass<RenderingT>,
  private val showRendering: @Composable() (RenderingT, ViewEnvironment) -> Unit
) : ViewFactory<RenderingT> {

  override fun buildView(
    initialRendering: RenderingT,
    initialViewEnvironment: ViewEnvironment,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    // There is currently no way to automatically generate an Android View directly from a
    // Composable function, so we need to use ViewGroup.setContent.
    val composeContainer = FrameLayout(contextForNewView)

    // This model will be used to feed state updates into the composition.
    val renderState = mutableStateOf<Pair<RenderingT, ViewEnvironment>?>(
        value = null,
        areEquivalent = StructurallyEqual
    )

    // Entry point to the composition.
    composeContainer.setContent(Recomposer.current()) {
      // Don't compose anything until we have the first value (which should happen in the initial
      // frame).
      val (rendering, environment) = renderState.value ?: return@setContent
      showRendering(rendering, environment)
    }

    composeContainer.bindShowRendering(
        initialRendering,
        initialViewEnvironment
    ) { rendering, environment ->
      renderState.value = Pair(rendering, environment)
    }
    return composeContainer
  }
}
