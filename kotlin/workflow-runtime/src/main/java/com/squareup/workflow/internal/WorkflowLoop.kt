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

import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select

internal interface WorkflowLoop {

  /**
   * Loops forever, or until the coroutine is cancelled, processing the workflow tree and emitting
   * updates by calling [onRendering] and [onOutput].
   *
   * This function is the lowest-level entry point into the runtime. Don't call this directly, instead
   * call [com.squareup.workflow.launchWorkflowIn].
   */
  @Suppress("LongParameterList")
  suspend fun <PropsT, StateT, OutputT : Any, RenderingT> runWorkflowLoop(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    props: Flow<PropsT>,
    initialSnapshot: Snapshot?,
    initialState: StateT? = null,
    onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
    onOutput: suspend (OutputT) -> Unit,
    diagnosticListener: WorkflowDiagnosticListener? = null
  ): Nothing
}

@UseExperimental(VeryExperimentalWorkflow::class)
internal object RealWorkflowLoop : WorkflowLoop {

  @UseExperimental(FlowPreview::class, ExperimentalCoroutinesApi::class)
  @Suppress("LongMethod")
  override suspend fun <PropsT, StateT, OutputT : Any, RenderingT> runWorkflowLoop(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    props: Flow<PropsT>,
    initialSnapshot: Snapshot?,
    initialState: StateT?,
    onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
    onOutput: suspend (OutputT) -> Unit,
    diagnosticListener: WorkflowDiagnosticListener?
  ): Nothing = coroutineScope {

    val inputsChannel = props.produceIn(this)
    inputsChannel.consume {
      var output: OutputT? = null
      var input: PropsT = inputsChannel.receive()
      var inputsClosed = false
      val idCounter = if (diagnosticListener != null) IdCounter() else null
      val rootNode = WorkflowNode(
          id = workflow.id(),
          workflow = workflow,
          initialProps = input,
          snapshot = initialSnapshot?.bytes,
          baseContext = coroutineContext,
          parentDiagnosticId = null,
          diagnosticListener = diagnosticListener,
          idCounter = idCounter,
          initialState = initialState
      )

      try {
        while (true) {
          coroutineContext.ensureActive()

          diagnosticListener?.onBeforeRenderPass(input)
          val rendering = rootNode.render(workflow, input)
          diagnosticListener?.apply {
            onAfterRenderPass(rendering)
            onBeforeSnapshotPass()
          }
          val snapshot = rootNode.snapshot(workflow)
          diagnosticListener?.onAfterSnapshotPass()

          onRendering(RenderingAndSnapshot(rendering, snapshot))
          output?.let { onOutput(it) }

          // Tick _might_ return an output, but if it returns null, it means the state or a child
          // probably changed, so we should re-render/snapshot and emit again.
          output = select {
            // Stop trying to read from the inputs channel after it's closed.
            if (!inputsClosed) {
              // TODO(https://github.com/square/workflow/issues/512) Replace with receiveOrClosed.
              @Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")
              inputsChannel.onReceiveOrNull { newInput ->
                diagnosticListener?.onPropsChanged(null, input, newInput, null, null)
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
}
