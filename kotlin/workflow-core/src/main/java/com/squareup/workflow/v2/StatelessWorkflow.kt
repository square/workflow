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
package com.squareup.workflow.v2

import com.squareup.workflow.legacy.Snapshot

/**
 * A convenience function to implement a [Workflow] that doesn't have any internal state. Such a
 * workflow doesn't need to worry about initial state or snapshotting, so the entire workflow can
 * be defined as a single [compose][Workflow.compose] function.
 *
 * Note that while a stateless workflow doesn't have any _internal_ state of its own, it may use
 * [input][InputT] received from its parent, and it may compose child workflows that do have their own
 * internal state.
 */
@Suppress("FunctionName")
fun <InputT : Any, OutputT : Any, RenderingT : Any> StatelessWorkflow(
  compose: (
    input: InputT,
    context: WorkflowContext<Unit, OutputT>
  ) -> RenderingT
): Workflow<InputT, Unit, OutputT, RenderingT> =
  object : Workflow<InputT, Unit, OutputT, RenderingT> {
    override fun initialState(input: InputT) = Unit

    override fun compose(
      input: InputT,
      state: Unit,
      context: WorkflowContext<Unit, OutputT>
    ): RenderingT = compose(input, context)

    override fun snapshotState(state: Unit) = Snapshot.EMPTY
    override fun restoreState(snapshot: Snapshot) = Unit
  }

/**
 * A version of [StatelessWorkflow] that doesn't have any input data either.
 */
@Suppress("FunctionName")
fun <OutputT : Any, RenderingT : Any> StatelessWorkflow(
  compose: (context: WorkflowContext<Unit, OutputT>) -> RenderingT
): Workflow<Unit, Unit, OutputT, RenderingT> =
  StatelessWorkflow { _: Unit, context: WorkflowContext<Unit, OutputT> ->
    compose(context)
  }
