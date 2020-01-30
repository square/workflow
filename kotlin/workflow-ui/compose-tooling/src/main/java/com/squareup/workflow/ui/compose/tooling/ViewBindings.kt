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
package com.squareup.workflow.ui.compose.tooling

import androidx.compose.Composable
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.compose.showRendering

/**
 * Draws this [ViewBinding] using a special preview `ViewRegistry`.
 *
 * Use inside `@Preview` Composable functions.
 */
@Composable fun <RenderingT : Any> ViewBinding<RenderingT>.preview(
  rendering: RenderingT,
  stubBinding: ViewBinding<Any> = PreviewStubViewBinding
) {
  val containerHints = PreviewContainerHints(stubBinding)
  showRendering(rendering, containerHints)
}
