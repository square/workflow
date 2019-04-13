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
package com.squareup.workflow

import com.squareup.workflow.WorkflowHost.Update
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn

/**
 * Given a stream of [InputT] values to use as inputs for the top-level [workflow], returns a
 * [Flow] that, when collected, will start [workflow] and emit its [Update]s. If the collection
 * is cancelled, the workflow will be terminated.
 *
 * The returned [Flow] is _not_ multicasted – **every subscription will start a new workflow
 * session.** It is recommended to use a multicasting operator on the resulting stream, such as
 * [Flow.broadcastIn][kotlinx.coroutines.flow.broadcastIn], to share the updates from a single
 * workflow session.
 *
 * The workflow's logic will run in the coroutine context of the collector.
 *
 * This operator is an alternative to using
 * [WorkflowHost.Factory][com.squareup.workflow.WorkflowHost.Factory] that is more convenient to
 * use with a stream
 * of inputs.
 *
 * @param snapshot If non-null, used to restore the workflow tree from a previous [Snapshot] emitted
 * in an [Update].
 */
@FlowPreview
fun <InputT : Any, OutputT : Any, RenderingT : Any> Flow<InputT>.flatMapWorkflow(
  workflow: Workflow<InputT, OutputT, RenderingT>,
  snapshot: Snapshot? = null
): Flow<Update<OutputT, RenderingT>> = flow {
  coroutineScope {
    val inputFlow = this@flatMapWorkflow
    val inputs = inputFlow.produceIn(this)
    runWorkflowTree(
        workflow = workflow.asStatefulWorkflow(),
        inputs = inputs,
        initialSnapshot = snapshot,
        onUpdate = ::emit
    )
  }
}
