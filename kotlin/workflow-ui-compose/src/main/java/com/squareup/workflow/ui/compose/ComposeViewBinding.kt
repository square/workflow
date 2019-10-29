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
package com.squareup.workflow.ui.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.ui.core.setContent
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.bindShowRendering
import kotlin.reflect.KClass

/**
 * Creates a [ViewBinding] that uses a [Composable] function to display the rendering.
 */
// See https://youtrack.jetbrains.com/issue/KT-31734
@Suppress("RemoveEmptyParenthesesFromAnnotationEntry")
inline fun <reified RenderingT : Any> bindCompose(
  noinline showRendering: @Composable() (RenderingT) -> Unit
): ViewBinding<RenderingT> = ComposeViewBinding(RenderingT::class) { rendering ->
  showRendering(rendering)
}

@PublishedApi
internal class ComposeViewBinding<RenderingT : Any>(
  override val type: KClass<RenderingT>,
  private val showRendering: @Composable() (RenderingT) -> Unit
) : ViewBinding<RenderingT> {

  override fun buildView(
    registry: ViewRegistry,
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    // TODO use GenerateView instead once it exists.
    val composeContainer = FrameLayout(contextForNewView)
    composeContainer.bindShowRendering(
        initialRendering,
        initialContainerHints
    ) { rendering, _ ->
      composeContainer.setContent {
        // TODO store ContainerHints in an Ambient.
        showRendering(rendering)
      }
    }
    return composeContainer
  }
}
