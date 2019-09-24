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
import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorker
import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorkflow
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Passthrough
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Sink
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Subtree
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Worker

/**
 * A [WorkflowDiagnosticListener] that records [WorkflowHierarchyDebugSnapshot]s and
 * [WorkflowUpdateDebugInfo]s and sends them to [debugger] after each render pass.
 */
@VeryExperimentalWorkflow
@Suppress("TooManyFunctions")
class DebugSnapshotRecordingListener(
  private val debugger: (WorkflowHierarchyDebugSnapshot, WorkflowUpdateDebugInfo?) -> Unit
) : WorkflowDiagnosticListener {

  private class WorkflowRecorder(
    val parentId: Long?,
    val type: String,
    val key: String
  ) {
    var props: Any? = null
    var state: Any? = null
    var rendering: Any? = null
    val childrenIds = mutableListOf<Long>()
    val workerIds = mutableListOf<Long>()

    fun snapshot(
      recordersById: Map<Long, WorkflowRecorder>,
      workersById: Map<Long, WorkerRecorder>
    ): WorkflowHierarchyDebugSnapshot =
      WorkflowHierarchyDebugSnapshot(
          workflowType = type,
          props = props,
          state = state,
          rendering = rendering,
          children = childrenIds.map { childId ->
            val child = recordersById.getValue(childId)
            ChildWorkflow(child.key, child.snapshot(recordersById, workersById))
          },
          workers = workerIds.map { workerId ->
            val worker = workersById.getValue(workerId)
            ChildWorker(worker.key, worker.description)
          }
      )
  }

  private class WorkerRecorder(
    val key: String,
    val description: String
  )

  private var rootRecorder: WorkflowRecorder? = null
  private val recordersById = mutableMapOf<Long, WorkflowRecorder>()
  private val workersById = mutableMapOf<Long, WorkerRecorder>()
  private var currentUpdate: WorkflowUpdateDebugInfo? = null

  override fun onRuntimeStopped() {
    recordersById.clear()
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
    val recorder = WorkflowRecorder(
        parentId, workflowType, key
    )
    recordersById[workflowId] = recorder

    if (parentId != null) {
      val parent = recordersById.getValue(parentId)
      parent.childrenIds += workflowId
    } else {
      require(rootRecorder == null)
      rootRecorder = recorder
    }
  }

  override fun onWorkflowStopped(workflowId: Long) {
    // Remove all this workflow's cached values.
    val recorder = recordersById.remove(workflowId)!!

    // We're tracking a bidirectional relationship, make sure we clean up both sides.
    if (recorder.parentId != null) {
      val parent = recordersById.getValue(recorder.parentId)
      parent.childrenIds.remove(workflowId)
    }
  }

  override fun onBeforeWorkflowRendered(
    workflowId: Long,
    props: Any?,
    state: Any?
  ) {
    recordersById.getValue(workflowId)
        .also {
          it.props = props
          it.state = state
        }
  }

  override fun onAfterWorkflowRendered(
    workflowId: Long,
    rendering: Any?
  ) {
    recordersById.getValue(workflowId)
        .rendering = rendering
  }

  override fun onAfterRenderPass(rendering: Any?) {
    val snapshot = rootRecorder!!.snapshot(recordersById, workersById)
    debugger(snapshot, currentUpdate)
    currentUpdate = null
  }

  // Update recording

  override fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) {
    val recorder = WorkerRecorder(
        key, description
    )
    workersById[workerId] = recorder
    recordersById.getValue(parentWorkflowId)
        .workerIds += workerId
  }

  override fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) {
    workersById.remove(workerId)
    recordersById.getValue(parentWorkflowId)
        .workerIds -= workerId
  }

  override fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) {
    val worker = workersById.getValue(workerId)
    val recorder = recordersById.getValue(parentWorkflowId)
    currentUpdate = WorkflowUpdateDebugInfo(
        workflowType = recorder.type,
        kind = Updated(Worker(worker.key, output))
    )
  }

  override fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) {
    val recorder = recordersById.getValue(workflowId)
    currentUpdate = WorkflowUpdateDebugInfo(
        workflowType = recorder.type,
        kind = Updated(Sink)
    )
  }

  override fun onWorkflowAction(
    workflowId: Long,
    action: WorkflowAction<*, *>,
    oldState: Any?,
    newState: Any?,
    output: Any?
  ) {
    val recorder = recordersById.getValue(workflowId)
    // If the workflow executing the workflow action is the root workflow, currentUpdate will
    // already contain the top-level update, so we don't have to do anything.
    val parent = recordersById[recorder.parentId] ?: return

    val sourceUpdate = currentUpdate!!
    currentUpdate = WorkflowUpdateDebugInfo(
        workflowType = parent.type,
        kind = if (output == null) {
          Passthrough(recorder.key, sourceUpdate)
        } else {
          Updated(Subtree(recorder.key, output, sourceUpdate))
        }
    )
  }
}
