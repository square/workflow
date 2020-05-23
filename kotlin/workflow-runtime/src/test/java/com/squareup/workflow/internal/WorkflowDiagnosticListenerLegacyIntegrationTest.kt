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
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.diagnostic.andThen
import com.squareup.workflow.internal.WorkflowDiagnosticListenerLegacyIntegrationTest.ZombieWorkflow.ChildSpec
import com.squareup.workflow.internal.WorkflowDiagnosticListenerLegacyIntegrationTest.ZombieWorkflow.Spec
import com.squareup.workflow.launchWorkflowIn
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateless
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.properties.Delegates.observable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for WorkflowDiagnosticListener with the legacy [launchWorkflowIn].
 */
class WorkflowDiagnosticListenerLegacyIntegrationTest {

  private val listener = RecordingDiagnosticListener()

  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  @Test fun `workflow tree structure changes emit events in order`() {
    val specChannel = Channel<Spec>()
    var spec by observable<Spec?>(null) { _, _, newValue ->
      newValue?.let { specChannel.offer(it) }
    }

    @Suppress("DEPRECATION")
    runBlocking {
      val (session, workflowScope) = launchWorkflowIn(
          scope = this,
          workflow = ZombieWorkflow,
          props = specChannel.consumeAsFlow()
      ) { session ->
        session.diagnosticListener = listener
        return@launchWorkflowIn Pair(session, this)
      }
      val renderings = session.renderingsAndSnapshots
          .map { it.rendering }
          .produceIn(this)

      yield()
      assertEquals(listOf("onRuntimeStarted"), listener.consumeEventNames())

      spec = Spec(state = "root state")

      // Initial events.
      assertEquals("state: initial state\n", renderings.receive())
      assertEquals(
          listOf(
              "onWorkflowStarted",
              "onBeforeRenderPass",
              "onBeforeWorkflowRendered",
              "onAfterWorkflowRendered",
              "onAfterRenderPass",
              "onBeforeSnapshotPass",
              "onAfterSnapshotPass"
          ), listener.consumeEventNames()
      )

      // Update workflow state.
      spec = spec?.copy(state = "different state")
      yield()
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
      spec = spec?.copy(
          children = listOf(
              ChildSpec("child1", Spec("child state"))
          )
      )
      yield()
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
      spec = spec?.copy(
          children = spec!!.children + ChildSpec("child2", Spec("child2 state"))
      )
      yield()
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
      spec = spec?.copy(children = spec!!.children.takeLast(1))
      yield()
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
      yield()
      assertEquals(
          listOf(
              "onWorkflowStopped",
              "onWorkflowStopped",
              "onRuntimeStopped"
          ), listener.consumeEventNames()
      )
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `workflow updates emit events in order`() {
    val propsChannel = Channel<String>(1).apply { offer("initial props") }
    val channel = Channel<String>()
    val worker = channel.asWorker()
    fun action(value: String) = action<Nothing, String> { setOutput("output:$value") }
    val workflow = Workflow.stateless<String, String, Unit> {
      runningWorker(worker, "key", ::action)
    }

    runBlocking {
      @Suppress("DEPRECATION")
      launchWorkflowIn(this, workflow, propsChannel.consumeAsFlow()) { session ->
        session.diagnosticListener = listener
            .andThen(SimpleLoggingDiagnosticListener())
        session.renderingsAndSnapshots.launchIn(this)
      }
      yield()
      yield()
      yield()
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
      yield()
      yield()
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

      propsChannel.send("new props")
      yield()
      yield()
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

      coroutineContext.cancelChildren()
      yield()
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
