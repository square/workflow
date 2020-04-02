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
package com.squareup.workflow.diagnostic

import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.WorkflowAction
import kotlinx.coroutines.CoroutineScope

/**
 * Returns a [WorkflowDiagnosticListener] that will delegate all method calls first to this
 * instance, and then to [next].
 */
fun WorkflowDiagnosticListener.andThen(
  next: WorkflowDiagnosticListener
): WorkflowDiagnosticListener {
  return (this as? ChainedDiagnosticListener
      ?: ChainedDiagnosticListener(this))
      .apply { addVisitor(next) }
}

@OptIn(VeryExperimentalWorkflow::class)
@Suppress("TooManyFunctions")
internal class ChainedDiagnosticListener(
  listener: WorkflowDiagnosticListener
) : WorkflowDiagnosticListener {

  private val visitors = mutableListOf(listener)

  fun addVisitor(listener: WorkflowDiagnosticListener) {
    if (listener is ChainedDiagnosticListener) {
      visitors.addAll(listener.visitors)
    } else {
      visitors += listener
    }
  }

  override fun onBeforeRenderPass(props: Any?) {
    visitors.forEach { it.onBeforeRenderPass(props) }
  }

  override fun onPropsChanged(
    workflowId: Long?,
    oldProps: Any?,
    newProps: Any?,
    oldState: Any?,
    newState: Any?
  ) {
    visitors.forEach { it.onPropsChanged(workflowId, oldProps, newProps, oldState, newState) }
  }

  override fun onBeforeWorkflowRendered(
    workflowId: Long,
    props: Any?,
    state: Any?
  ) {
    visitors.forEach { it.onBeforeWorkflowRendered(workflowId, props, state) }
  }

  override fun onAfterWorkflowRendered(
    workflowId: Long,
    rendering: Any?
  ) {
    visitors.forEach { it.onAfterWorkflowRendered(workflowId, rendering) }
  }

  override fun onAfterRenderPass(rendering: Any?) {
    visitors.forEach { it.onAfterRenderPass(rendering) }
  }

  override fun onBeforeSnapshotPass() {
    visitors.forEach { it.onBeforeSnapshotPass() }
  }

  override fun onAfterSnapshotPass() {
    visitors.forEach { it.onAfterSnapshotPass() }
  }

  override fun onRuntimeStarted(
    workflowScope: CoroutineScope,
    rootWorkflowType: String
  ) {
    visitors.forEach { it.onRuntimeStarted(workflowScope, rootWorkflowType) }
  }

  override fun onRuntimeStopped() {
    visitors.forEach { it.onRuntimeStopped() }
  }

  override fun onWorkflowStarted(
    workflowId: Long,
    parentId: Long?,
    workflowType: String,
    key: String,
    initialProps: Any?,
    initialState: Any?,
    restoredFromSnapshot: Boolean
  ) {
    visitors.forEach {
      it.onWorkflowStarted(
          workflowId, parentId, workflowType, key, initialProps, initialState, restoredFromSnapshot
      )
    }
  }

  override fun onWorkflowStopped(workflowId: Long) {
    visitors.forEach { it.onWorkflowStopped(workflowId) }
  }

  override fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) {
    visitors.forEach { it.onWorkerStarted(workerId, parentWorkflowId, key, description) }
  }

  override fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) {
    visitors.forEach { it.onWorkerStopped(workerId, parentWorkflowId) }
  }

  override fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) {
    visitors.forEach { it.onWorkerOutput(workerId, parentWorkflowId, output) }
  }

  override fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) {
    visitors.forEach { it.onSinkReceived(workflowId, action) }
  }

  override fun onWorkflowAction(
    workflowId: Long,
    action: WorkflowAction<*, *>,
    oldState: Any?,
    newState: Any?,
    output: Any?
  ) {
    visitors.forEach { it.onWorkflowAction(workflowId, action, oldState, newState, output) }
  }
}
