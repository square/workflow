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
 * A [WorkflowDiagnosticListener] that just prints all events using [println].
 */
@OptIn(VeryExperimentalWorkflow::class)
@Suppress("TooManyFunctions")
open class SimpleLoggingDiagnosticListener : WorkflowDiagnosticListener {

  /**
   * Called with descriptions of every event. Default implementation just calls [kotlin.io.println].
   */
  protected open fun println(text: String) {
    kotlin.io.println(text)
  }

  override fun onBeforeRenderPass(props: Any?) {
    println("onBeforeRenderPass($props)")
  }

  override fun onPropsChanged(
    workflowId: Long?,
    oldProps: Any?,
    newProps: Any?,
    oldState: Any?,
    newState: Any?
  ) {
    println("onPropsChanged($workflowId, $oldProps, $newProps, $oldState, $newState)")
  }

  override fun onBeforeWorkflowRendered(
    workflowId: Long,
    props: Any?,
    state: Any?
  ) {
    println("onBeforeWorkflowRendered($workflowId, $props, $state)")
  }

  override fun onAfterWorkflowRendered(
    workflowId: Long,
    rendering: Any?
  ) {
    println("onAfterWorkflowRendered($workflowId, $rendering)")
  }

  override fun onAfterRenderPass(rendering: Any?) {
    println("onAfterRenderPass($rendering)")
  }

  override fun onBeforeSnapshotPass() {
    println("onBeforeSnapshotPass")
  }

  override fun onAfterSnapshotPass() {
    println("onAfterSnapshotPass")
  }

  override fun onRuntimeStarted(
    workflowScope: CoroutineScope,
    rootWorkflowType: String
  ) {
    println("onRuntimeStarted($workflowScope, $rootWorkflowType)")
  }

  override fun onRuntimeStopped() {
    println("onRuntimeStopped")
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
    println(
        "onWorkflowStarted($workflowId, $parentId, $workflowType, $key, $initialProps, " +
            "$initialState, $restoredFromSnapshot)"
    )
  }

  override fun onWorkflowStopped(workflowId: Long) {
    println("onWorkflowStopped($workflowId)")
  }

  override fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) {
    println("onWorkerStarted($workerId, $parentWorkflowId, $key, $description)")
  }

  override fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) {
    println("onWorkerStopped($workerId, $parentWorkflowId)")
  }

  override fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) {
    println("onWorkerOutput($workerId, $parentWorkflowId, $output)")
  }

  override fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) {
    println("onSinkReceived($workflowId, $action)")
  }

  override fun onWorkflowAction(
    workflowId: Long,
    action: WorkflowAction<*, *>,
    oldState: Any?,
    newState: Any?,
    output: Any?
  ) {
    println("onWorkflowAction($workflowId, $action, $oldState, $newState, $output)")
  }
}
