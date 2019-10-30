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
import com.squareup.workflow.ui.ViewRegistry

/**
 * Renders [rendering] into the composition using this [ContainerHints]'
 * [ViewRegistry][com.squareup.workflow.ui.ViewRegistry] to generate the view.
 *
 * ## Example
 *
 * ```
 * data class FramedRendering(
 *   val borderColor: Color,
 *   val child: Any
 * )
 *
 * val FramedContainerBinding = bindCompose { rendering: FramedRendering, hints: ContainerHints ->
 *   Surface(border = Border(rendering.borderColor, 8.dp)) {
 *     hints.showRendering(rendering.child)
 *   }
 * }
 * ```
 */
@Composable fun ContainerHints.showRendering(rendering: Any) {
  val viewRegistry = remember(this) { this[ViewRegistry] }
  viewRegistry.showRendering(rendering, this)
}
