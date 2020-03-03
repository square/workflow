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
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorker
import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorkflow
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(VeryExperimentalWorkflow::class)
class DebugSnapshotRecordingListenerTest {

  private var snapshot: WorkflowHierarchyDebugSnapshot? = null
  private var update: WorkflowUpdateDebugInfo? = null
  private val listener = DebugSnapshotRecordingListener { snapshot, update ->
    this.snapshot = snapshot
    this.update = update
  }

  @Test fun `produces valid debug snapshot single workflow`() {
    // Emit the events required to generate a snapshot.
    performRenderPassSingleWorkflow()

    assertEquals(
        WorkflowHierarchyDebugSnapshot(
            workflowType = "type",
            props = "props",
            state = "state",
            rendering = "rendering",
            children = emptyList(),
            workers = emptyList()
        ), snapshot
    )
  }

  @Test fun `produces valid debug snapshot parent child workflow`() {
    performRenderPassSingleChild()

    assertEquals(
        WorkflowHierarchyDebugSnapshot(
            workflowType = "type",
            props = "props",
            state = "state",
            rendering = "rendering",
            children = listOf(
                ChildWorkflow(
                    "key2",
                    WorkflowHierarchyDebugSnapshot(
                        workflowType = "type2",
                        props = "props2",
                        state = "state2",
                        rendering = "rendering2",
                        children = emptyList(),
                        workers = emptyList()
                    )
                )
            ),
            workers = emptyList()
        ), snapshot
    )
  }

  @Test fun `produces valid debug snapshot workflow stopped`() {
    performRenderPassSingleChild()
    performRenderPassRemoveSingleChild()

    assertEquals(
        WorkflowHierarchyDebugSnapshot(
            workflowType = "type",
            props = "props",
            state = "state",
            rendering = "rendering",
            children = emptyList(),
            workers = emptyList()
        ), snapshot
    )
  }

  @Test fun `produces valid debug snapshot worker`() {
    performRenderPassSingleWorker()

    assertEquals(
        WorkflowHierarchyDebugSnapshot(
            workflowType = "type",
            props = "props",
            state = "state",
            rendering = "rendering",
            children = emptyList(),
            workers = listOf(
                ChildWorker("key2", "description")
            )
        ), snapshot
    )
  }

  @Test fun `produces valid debug snapshot worker stopped`() {
    performRenderPassSingleWorker()
    performRenderPassRemoveSingleWorker()

    assertEquals(
        WorkflowHierarchyDebugSnapshot(
            workflowType = "type",
            props = "props",
            state = "state",
            rendering = "rendering",
            children = emptyList(),
            workers = emptyList()
        ), snapshot
    )
  }

  @Test fun `produces valid update info from sink`() {
    performRenderPassSingleWorkflow()
    listener.onSinkReceived(0, noAction<Any, Any>())
    listener.onWorkflowAction(0, noAction<Any, Any>(), "old", "new", "parent")
    performRenderPassSingleWorkflow(start = false)

    assertEquals(
        WorkflowUpdateDebugInfo(
            "type",
            Kind.Updated(Source.Sink)
        ),
        update
    )
  }

  @Test fun `produces valid update info from worker`() {
    performRenderPassSingleWorker()
    listener.onWorkerOutput(1, 0, "output")
    listener.onWorkflowAction(0, noAction<Any, Any>(), "old", "new", "parent")
    performRenderPassSingleWorker(start = false)

    assertEquals(
        WorkflowUpdateDebugInfo(
            "type",
            Kind.Updated(Source.Worker("key2", "output"))
        ),
        update
    )
  }

  @Test fun `produces valid update info from subtree`() {
    performRenderPassSingleChild()
    listener.onSinkReceived(1, noAction<Any, Any>())
    listener.onWorkflowAction(1, noAction<Any, Any>(), "old child", "new child", "child")
    listener.onWorkflowAction(0, noAction<Any, Any>(), "old", "new", "parent")
    performRenderPassSingleChild(start = false)

    assertEquals(
        WorkflowUpdateDebugInfo(
            "type",
            Kind.Updated(
                Source.Subtree(
                    "key2", "child",
                    WorkflowUpdateDebugInfo(
                        "type2",
                        Kind.Updated(Source.Sink)
                    )
                )
            )
        ),
        update
    )
  }

  @Test fun `produces valid update info from passthrough`() {
    performRenderPassSingleChild()
    listener.onSinkReceived(1, noAction<Any, Any>())
    listener.onWorkflowAction(1, noAction<Any, Any>(), "old child", "new child", null)
    listener.onWorkflowAction(0, noAction<Any, Any>(), "old", "new", null)
    performRenderPassSingleChild(start = false)

    assertEquals(
        WorkflowUpdateDebugInfo(
            "type",
            Kind.Passthrough(
                "key2",
                WorkflowUpdateDebugInfo(
                    "type2",
                    Kind.Updated(Source.Sink)
                )
            )
        ),
        update
    )
  }

  private fun performRenderPassSingleWorkflow(start: Boolean = true) {
    if (start) listener.onWorkflowStarted(0, null, "type", "key", "props", "state", false)
    listener.onBeforeRenderPass("props")
    listener.onBeforeWorkflowRendered(0, "props", "state")
    listener.onAfterWorkflowRendered(0, "rendering")
    listener.onAfterRenderPass("rendering")
  }

  private fun performRenderPassSingleChild(start: Boolean = true) {
    if (start) listener.onWorkflowStarted(0, null, "type", "key", "props", "state", false)
    listener.onBeforeRenderPass("props")
    listener.onBeforeWorkflowRendered(0, "props", "state")
    listener.onWorkflowStarted(1, 0, "type2", "key2", "props2", "state2", false)
    listener.onBeforeWorkflowRendered(1, "props2", "state2")
    listener.onAfterWorkflowRendered(1, "rendering2")
    listener.onAfterWorkflowRendered(0, "rendering")
    listener.onAfterRenderPass("rendering")
  }

  private fun performRenderPassRemoveSingleChild() {
    listener.onBeforeRenderPass("props")
    listener.onBeforeWorkflowRendered(0, "props", "state")
    listener.onWorkflowStopped(1)
    listener.onAfterWorkflowRendered(0, "rendering")
    listener.onAfterRenderPass("rendering")
  }

  private fun performRenderPassSingleWorker(start: Boolean = true) {
    if (start) listener.onWorkflowStarted(0, null, "type", "key", "props", "state", false)
    listener.onBeforeRenderPass("props")
    listener.onBeforeWorkflowRendered(0, "props", "state")
    listener.onWorkerStarted(1, 0, "key2", "description")
    listener.onAfterWorkflowRendered(0, "rendering")
    listener.onAfterRenderPass("rendering")
  }

  private fun performRenderPassRemoveSingleWorker() {
    listener.onBeforeRenderPass("props")
    listener.onBeforeWorkflowRendered(0, "props", "state")
    listener.onWorkerStopped(1, 0)
    listener.onAfterWorkflowRendered(0, "rendering")
    listener.onAfterRenderPass("rendering")
  }
}
