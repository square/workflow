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

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.asWorker
import com.squareup.workflow.internal.WorkflowDiagnosticListenerIntegrationTest.ZombieWorkflow.ChildSpec
import com.squareup.workflow.internal.WorkflowDiagnosticListenerIntegrationTest.ZombieWorkflow.Spec
import com.squareup.workflow.renderChild
import com.squareup.workflow.renderWorkflowIn
import com.squareup.workflow.stateless
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkflowDiagnosticListenerIntegrationTest {

  private val listener = RecordingDiagnosticListener()

  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  @Test fun `workflow tree structure changes emit events in order`() = runBlockingTest {
    val spec = MutableStateFlow(Spec(state = "root state"))
    val workflowScope = CoroutineScope(coroutineContext + Job(parent = coroutineContext[Job]))
    val renderings =
      renderWorkflowIn(ZombieWorkflow, workflowScope, spec, diagnosticListener = listener) {}
          .map { it.rendering }
          .produceIn(this)

    // Initial events.
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onRuntimeStarted",
            "onWorkflowStarted",
            "onBeforeRenderPass",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterRenderPass",
            "onBeforeSnapshotPass",
            "onAfterSnapshotPass"
        ), listener.consumeEventNames()
    )
    assertEquals("state: initial state\n", renderings.receive())

    // Update workflow state.
    spec.value = spec.value.copy(state = "different state")
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onPropsChanged",
            "onBeforeRenderPass",
            "onPropsChanged",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterRenderPass",
            "onBeforeSnapshotPass",
            "onAfterSnapshotPass"
        ), listener.consumeEventNames()
    )

    // Add a child.
    spec.value = spec.value.copy(
        children = listOf(
            ChildSpec("child1", Spec("child state"))
        )
    )
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onPropsChanged",
            "onBeforeRenderPass",
            "onPropsChanged",
            "onBeforeWorkflowRendered",
            "onWorkflowStarted",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterRenderPass",
            "onBeforeSnapshotPass",
            "onAfterSnapshotPass"
        ), listener.consumeEventNames()
    )

    // Add a sibling.
    spec.value = spec.value.copy(
        children = spec.value.children + ChildSpec("child2", Spec("child2 state"))
    )
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onPropsChanged",
            "onBeforeRenderPass",
            "onPropsChanged",
            "onBeforeWorkflowRendered",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onWorkflowStarted",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterRenderPass",
            "onBeforeSnapshotPass",
            "onAfterSnapshotPass"
        ), listener.consumeEventNames()
    )

    // Remove a child.
    spec.value = spec.value.copy(children = spec.value.children.takeLast(1))
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onPropsChanged",
            "onBeforeRenderPass",
            "onPropsChanged",
            "onBeforeWorkflowRendered",
            "onBeforeWorkflowRendered",
            "onAfterWorkflowRendered",
            "onAfterWorkflowRendered",
            "onWorkflowStopped",
            "onAfterRenderPass",
            "onBeforeSnapshotPass",
            "onAfterSnapshotPass"
        ), listener.consumeEventNames()
    )

    workflowScope.cancel()
    advanceUntilIdle()
    assertEquals(
        listOf(
            "onWorkflowStopped",
            "onWorkflowStopped",
            "onRuntimeStopped"
        ), listener.consumeEventNames()
    )

    // Cleanup the renderings coroutine so runBlockingTest is happy.
    renderings.cancel()
  }

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `workflow updates emit events in order`() {
    val props = MutableStateFlow("initial props")
    val channel = Channel<String>()
    val worker = channel.asWorker()
    fun action(value: String) = action<Nothing, String> { setOutput("output:$value") }
    val workflow = Workflow.stateless<String, String, Unit> {
      runningWorker(worker, "key", ::action)
    }

    runBlockingTest {
      val workflowScope = CoroutineScope(coroutineContext + Job(parent = coroutineContext[Job]))
      renderWorkflowIn(workflow, workflowScope, props, diagnosticListener = listener) {}
      advanceUntilIdle()
      assertEquals(
          listOf(
              "onRuntimeStarted",
              "onWorkflowStarted",
              "onBeforeRenderPass",
              "onBeforeWorkflowRendered",
              "onWorkerStarted",
              "onAfterWorkflowRendered",
              "onAfterRenderPass",
              "onBeforeSnapshotPass",
              "onAfterSnapshotPass"
          ),
          actual = listener.consumeEventNames()
      )

      channel.send("foo")
      advanceUntilIdle()
      assertEquals(
          listOf(
              "onWorkerOutput",
              "onWorkflowAction",
              "onBeforeRenderPass",
              "onBeforeWorkflowRendered",
              "onAfterWorkflowRendered",
              "onAfterRenderPass",
              "onBeforeSnapshotPass",
              "onAfterSnapshotPass"
          ), listener.consumeEventNames()
      )

      props.value = "new props"
      advanceUntilIdle()
      assertEquals(
          listOf(
              "onPropsChanged",
              "onBeforeRenderPass",
              "onPropsChanged",
              "onBeforeWorkflowRendered",
              "onAfterWorkflowRendered",
              "onAfterRenderPass",
              "onBeforeSnapshotPass",
              "onAfterSnapshotPass"
          ), listener.consumeEventNames()
      )

      workflowScope.cancel()
      advanceUntilIdle()
      assertEquals(
          listOf(
              "onWorkerStopped",
              "onWorkflowStopped",
              "onRuntimeStopped"
          ), listener.consumeEventNames()
      )
    }
  }

  /**
   * A workflow that just sets up children based on its [Spec] props.
   */
  private object ZombieWorkflow : StatefulWorkflow<Spec, String, Nothing, String>() {

    data class Spec(
      val state: String,
      val children: List<ChildSpec> = emptyList()
    )

    data class ChildSpec(
      val key: String,
      val spec: Spec
    )

    override fun initialState(
      props: Spec,
      snapshot: Snapshot?
    ): String = "initial state"

    override fun onPropsChanged(
      old: Spec,
      new: Spec,
      state: String
    ): String = new.state

    override fun render(
      props: Spec,
      state: String,
      context: RenderContext<String, Nothing>
    ): String {
      val childRenderings = props.children.map { childSpec ->
        "  ${childSpec.key}: " + context.renderChild(ZombieWorkflow, childSpec.spec, childSpec.key)
      }

      return "state: $state\n" + childRenderings.joinToString(separator = "\n")
    }

    override fun snapshotState(state: String): Snapshot = Snapshot.EMPTY
  }
}
