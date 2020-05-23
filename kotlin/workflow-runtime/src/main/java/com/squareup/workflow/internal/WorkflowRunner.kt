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
package com.squareup.workflow.internal

import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, VeryExperimentalWorkflow::class)
internal open class WorkflowRunner<PropsT, OutputT : Any, RenderingT>(
  scope: CoroutineScope,
  wf: Workflow<PropsT, OutputT, RenderingT>,
  props: StateFlow<PropsT>,
  initialSnapshot: Snapshot?,
  private val diagnosticListener: WorkflowDiagnosticListener?
) {
  private val workflow = wf.asStatefulWorkflow()
  private val idCounter = if (diagnosticListener != null) IdCounter() else null
  private var currentProps: PropsT = props.value

  @OptIn(FlowPreview::class)
  private val propsChannel = props.produceIn(scope)

  private val rootNode = WorkflowNode(
      id = workflow.id(),
      workflow = workflow,
      initialProps = currentProps,
      snapshot = initialSnapshot?.bytes?.takeUnless { it.size == 0 },
      baseContext = scope.coroutineContext,
      workerContext = EmptyCoroutineContext,
      parentDiagnosticId = null,
      diagnosticListener = diagnosticListener,
      idCounter = idCounter,
      initialState = null // TODO
  )

  suspend fun consumeNextProps() {
    onNewProps(propsChannel.receive())
  }

  fun nextRendering(): RenderingAndSnapshot<RenderingT> {
    diagnosticListener?.onBeforeRenderPass(currentProps)
    val rendering = doRender(rootNode, workflow, currentProps)
    diagnosticListener?.apply {
      onAfterRenderPass(rendering)
      onBeforeSnapshotPass()
    }
    val snapshot = rootNode.snapshot(workflow)
    diagnosticListener?.onAfterSnapshotPass()
    return RenderingAndSnapshot(rendering, snapshot)
  }

  suspend fun nextOutput(): OutputT? {
    // Tick _might_ return an output, but if it returns null, it means the state or a child
    // probably changed, so we should re-render/snapshot and emit again.
    return select {
      // Stop trying to read from the inputs channel after it's closed.
      if (!propsChannel.isClosedForReceive) {
        // TODO(https://github.com/square/workflow/issues/512) Replace with receiveOrClosed.
        @Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")
        propsChannel.onReceiveOrNull { newProps ->
          newProps?.let(::onNewProps)
          // No output. Returning from the select will go to the top of the loop to do another
          // render pass.
          return@onReceiveOrNull null
        }
      }

      // Tick the workflow tree.
      rootNode.tick(this)
    }
  }

  fun cancelRootNode() {
    rootNode.cancel()
  }

  private fun onNewProps(newProps: PropsT) {
    if (currentProps != newProps) {
      diagnosticListener?.onPropsChanged(null, currentProps, newProps, null, null)
      currentProps = newProps
    }
  }

  protected open fun <PropsT, OutputT : Any, RenderingT> doRender(
    rootNode: WorkflowNode<PropsT, *, OutputT, RenderingT>,
    workflow: StatefulWorkflow<PropsT, *, OutputT, RenderingT>,
    props: PropsT
  ): RenderingT = rootNode.render(workflow, props)
}
