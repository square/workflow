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
package com.squareup.workflow.internal

import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.RenderingAndSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select

/**
 * Loops forever, or until the coroutine is cancelled, processing the workflow tree and emitting
 * updates by calling [onRendering] and [onOutput].
 *
 * This function is the lowest-level entry point into the runtime. Don't call this directly, instead
 * use [com.squareup.workflow.WorkflowHost.Factory] to create a
 * [com.squareup.workflow.WorkflowHost].
 */
@UseExperimental(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal suspend fun <InputT, StateT, OutputT : Any, RenderingT> runWorkflowLoop(
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  inputs: Flow<InputT>,
  initialSnapshot: Snapshot?,
  initialState: StateT? = null,
  onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
  onOutput: suspend (OutputT) -> Unit
): Nothing = coroutineScope {
  val inputsChannel = inputs.produceIn(this)
  inputsChannel.consume {
    var output: OutputT? = null
    var input: InputT = inputsChannel.receive()
    var inputsClosed = false
    val rootNode = WorkflowNode(
        id = workflow.id(),
        workflow = workflow,
        initialInput = input,
        snapshot = initialSnapshot,
        baseContext = coroutineContext,
        initialState = initialState
    )

    try {
      while (true) {
        coroutineContext.ensureActive()

        val rendering = rootNode.render(workflow, input)
        val snapshot = rootNode.snapshot(workflow)

        onRendering(RenderingAndSnapshot(rendering, snapshot))
        output?.let { onOutput(it) }

        // Tick _might_ return an output, but if it returns null, it means the state or a child
        // probably changed, so we should re-render/snapshot and emit again.
        output = select {
          // Stop trying to read from the inputs channel after it's closed.
          if (!inputsClosed) {
            @Suppress("EXPERIMENTAL_API_USAGE")
            inputsChannel.onReceiveOrNull { newInput ->
              if (newInput == null) {
                inputsClosed = true
              } else {
                input = newInput
              }
              // No output. Returning from the select will go to the top of the loop to do another
              // render pass.
              return@onReceiveOrNull null
            }
          }

          // Tick the workflow tree.
          rootNode.tick(this) { it }
        }
      }
      // Compiler gets confused, and thinks both that this throw is unreachable, and without the
      // throw that the infinite while loop will exit normally and thus need a return statement.
      @Suppress("UNREACHABLE_CODE", "ThrowableNotThrown")
      throw AssertionError()
    } finally {
      // There's a potential race condition if the producer coroutine is cancelled before it has a
      // chance to enter the try block, since we can't use CoroutineStart.ATOMIC. However, until we
      // actually see this cause problems, I'm not too worried about it.
      // See https://github.com/Kotlin/kotlinx.coroutines/issues/845
      rootNode.cancel()
    }
  }
}
