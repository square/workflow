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

import androidx.compose.Model
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.action
import com.squareup.workflow.compose.ComposeWorkflowImpl.State
import com.squareup.workflow.contraMap

internal class ComposeWorkflowImpl<PropsT, OutputT : Any>(
  private val workflow: ComposeWorkflow<PropsT, OutputT>
) : StatefulWorkflow<PropsT, State<PropsT>, OutputT, ComposeRendering>() {

  /**
   * Holds the props so that Compose will automatically re-compose when the props changes. This way
   * we can create and cache a single [ComposeRendering] instead of making a new one on every
   * render pass.
   */
  @Model
  class PropsHolder<PropsT>(var props: PropsT)

  data class State<PropsT>(val propsHolder: PropsHolder<PropsT>) {
    var rendering: ComposeRendering? = null
  }

  override fun initialState(
    props: PropsT,
    snapshot: Snapshot?
  ): State<PropsT> = State(PropsHolder(props))

  override fun onPropsChanged(
    old: PropsT,
    new: PropsT,
    state: State<PropsT>
  ): State<PropsT> {
    state.propsHolder.props = new
    return state
  }

  override fun render(
    props: PropsT,
    state: State<PropsT>,
    context: RenderContext<State<PropsT>, OutputT>
  ): ComposeRendering = state.rendering ?: run {
    // The first render pass needs to cache the Compose rendering. The sink is reusable, so we
    // can just pass the same one every time, and onPropsChanged will ensure the rendering is
    // re-composed when the props changes.
    val outputSink: Sink<OutputT> = context.actionSink.contraMap(::forwardOutput)
    ComposeRendering { hints ->
      // Important: Use the props from the PropsHolder, _not_ the one passed into render.
      workflow.render(state.propsHolder.props, outputSink, hints)
    }.also { state.rendering = it }
  }

  // Compiler bug doesn't let us call Snapshot.EMPTY.
  override fun snapshotState(state: State<PropsT>): Snapshot = Snapshot.of("")

  private fun forwardOutput(output: OutputT) = action { setOutput(output) }
}
