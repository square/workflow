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
import androidx.ui.core.setContent
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.bindShowRendering
import kotlin.reflect.KClass

/**
 * Creates a [ViewBinding] that uses a [Composable] function to display the rendering.
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
 *
 * ## Implementing Containers
 *
 * Views that act as containers (i.e. they delegate to the
 * [ViewRegistry][com.squareup.workflow.ui.ViewRegistry] to render other rendering types) may use
 * [ContainerHints.showRendering] to compose child renderings. See the kdoc on that function for
 * more information.
 */
inline fun <reified RenderingT : Any> bindCompose(
  noinline showRendering: @Composable() (RenderingT, ContainerHints) -> Unit
): ViewBinding<RenderingT> = ComposeViewBinding(RenderingT::class) { rendering, hints ->
  showRendering(rendering, hints)
}

@PublishedApi
internal class ComposeViewBinding<RenderingT : Any>(
  override val type: KClass<RenderingT>,
  val showRendering: @Composable() (RenderingT, ContainerHints) -> Unit
) : ViewBinding<RenderingT> {

  override fun buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    // There is currently no way to automatically generate an Android View directly from a
    // Composable function, so we need to use ViewGroup.setContent.
    val composeContainer = FrameLayout(contextForNewView)
    composeContainer.bindShowRendering(
        initialRendering,
        initialContainerHints
    ) { rendering, hints ->
      composeContainer.setContent {
        showRendering(rendering, hints)
      }
    }
    return composeContainer
  }
}
