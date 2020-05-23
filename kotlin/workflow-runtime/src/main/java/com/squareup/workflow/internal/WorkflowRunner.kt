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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, VeryExperimentalWorkflow::class)
internal class WorkflowRunner<PropsT, OutputT : Any, RenderingT>(
  scope: CoroutineScope,
  protoWorkflow: Workflow<PropsT, OutputT, RenderingT>,
  props: StateFlow<PropsT>,
  initialSnapshot: Snapshot?,
  private val diagnosticListener: WorkflowDiagnosticListener?
) {
  private val workflow = protoWorkflow.asStatefulWorkflow()
  private val idCounter = if (diagnosticListener != null) IdCounter() else null
  private var currentProps: PropsT = props.value

  // Props is a StateFlow, it will immediately produce an item. Without additional handling, the
  // first call to nextOutput will see that new props value and trigger another render pass, which
  // means that every workflow runtime would actually start with two render passes. To avoid that,
  // we skip that first value. We can't just to drop(1) however, since there's a possibility that
  // the props flow emitted a new value in the time between the above initialization of currentProps
  // and the produceIn coroutine starting to collect from the props. Using dropWhile ensures that
  // we don't miss the new props in that case, but that we don't trigger the double render pass
  // unnecessarily. Note that currentProps is only set by nextOutput receiving from this channel,
  // which can't happen until the dropWhile predicate evaluates to false, after which the dropWhile
  // predicate will never be invoked again, so it's fine to read the mutable value here.
  @OptIn(FlowPreview::class)
  private val propsChannel = props.dropWhile { it == currentProps }
      .produceIn(scope)

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
      // TODO(https://github.com/square/workflow/issues/1192) Migrate testing infra.
      initialState = null
  )

  /**
   * Perform a render pass and a snapshot pass and return the results.
   *
   * This method must be called before the first call to [nextOutput], and must be called again
   * between every subsequent call to [nextOutput].
   */
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

  // Tick _might_ return an output, but if it returns null, it means the state or a child
  // probably changed, so we should re-render/snapshot and emit again.
  suspend fun nextOutput(): OutputT? = select {
    // Stop trying to read from the inputs channel after it's closed.
    if (!propsChannel.isClosedForReceive) {
      // TODO(https://github.com/square/workflow/issues/512) Replace with receiveOrClosed.
      @Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")
      propsChannel.onReceiveOrNull { newProps ->
        newProps?.let(::onNewProps)
        // Return null to tell the caller to do another render pass, but not emit an output.
        return@onReceiveOrNull null
      }
    }

    // Tick the workflow tree.
    rootNode.tick(this)
  }

  fun cancelRuntime(cause: CancellationException? = null) {
    rootNode.cancel(cause)
  }

  private fun onNewProps(newProps: PropsT) {
    if (currentProps != newProps) {
      diagnosticListener?.onPropsChanged(null, currentProps, newProps, null, null)
      currentProps = newProps
    }
  }

  // TODO(https://github.com/square/workflow/issues/1192) Migrate testing infra.
  private fun <PropsT, OutputT : Any, RenderingT> doRender(
    rootNode: WorkflowNode<PropsT, *, OutputT, RenderingT>,
    workflow: StatefulWorkflow<PropsT, *, OutputT, RenderingT>,
    props: PropsT
  ): RenderingT = rootNode.render(workflow, props)
}
