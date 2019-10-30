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
package com.squareup.workflow.ui.compose

import androidx.compose.Composable
import androidx.compose.remember
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry

/**
 * Renders [rendering] into the composition using this [ViewRegistry] to determine how to draw it.
 *
 * To display a nested rendering from a [Composable view binding][bindCompose], use
 * [ContainerHints.showRendering].
 *
 * @see ContainerHints.showRendering
 * @see ViewBinding.showRendering
 */
@Composable fun ViewRegistry.showRendering(
  rendering: Any,
  hints: ContainerHints
) {
  val renderingType = rendering::class
  val binding: ViewBinding<Any> = remember(renderingType) { getBindingFor(renderingType) }
  binding.showRendering(rendering = rendering, hints = hints)
}
