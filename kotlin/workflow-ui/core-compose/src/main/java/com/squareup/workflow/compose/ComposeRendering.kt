/*
 * Copyright 2020 Square Inc.
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
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.workflow.compose

import androidx.compose.Composable
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.compose.ComposeRendering.Companion.Binding
import com.squareup.workflow.ui.compose.bindCompose

/**
 * A workflow rendering that renders itself using a [Composable] function.
 *
 * This is the rendering type of [ComposeWorkflow]. To stub out [ComposeWorkflow]s in `RenderTester`
 * tests, use [NoopRendering].
 *
 * To use this type, make sure your `ViewRegistry` registers [Binding].
 */
class ComposeRendering internal constructor(
  internal val render: @Composable() (ContainerHints) -> Unit
) {
  companion object {
    /**
     * A [ViewBinding] that renders a [ComposeRendering].
     */
    val Binding: ViewBinding<ComposeRendering> =
      bindCompose { rendering, hints ->
        rendering.render(hints)
      }

    /**
     * A [ComposeRendering] that doesn't do anything. Useful for unit testing.
     */
    val NoopRendering = ComposeRendering {}
  }
}
