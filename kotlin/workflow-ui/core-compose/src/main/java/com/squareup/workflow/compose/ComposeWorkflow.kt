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
package com.squareup.workflow.compose

import androidx.compose.Composable
import com.squareup.workflow.Sink
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.ui.ContainerHints

/**
 * A stateless [Workflow][com.squareup.workflow.Workflow] that [renders][render] itself as
 * [Composable] function. Effectively defines an inline
 * [ViewBinding][com.squareup.workflow.ui.ViewBinding].
 *
 * This workflow does not have access to a [RenderContext][com.squareup.workflow.RenderContext]
 * since render contexts are only valid during render passes, and this workflow's [render] method
 * is invoked after the render pass, when view bindings are being shown.
 */
abstract class ComposeWorkflow<in PropsT, out OutputT : Any> :
    Workflow<PropsT, OutputT, ComposeRendering> {

  /**
   * Renders [props] using Compose. This function will be called to update the UI whenever the
   * [props] or [containerHints] changes.
   *
   * @param props The data to render.
   * @param outputSink A [Sink] that can be used from UI event handlers to send outputs to this
   * workflow's parent.
   * @param containerHints The [ContainerHints] passed down through the `ViewBinding` pipeline.
   */
  @Composable abstract fun render(
    props: PropsT,
    outputSink: Sink<OutputT>,
    containerHints: ContainerHints
  )

  override fun asStatefulWorkflow(): StatefulWorkflow<PropsT, *, OutputT, ComposeRendering> =
    ComposeWorkflowImpl(this)
}
