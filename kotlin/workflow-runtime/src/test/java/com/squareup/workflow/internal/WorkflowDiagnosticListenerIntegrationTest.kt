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
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.asWorker
import com.squareup.workflow.diagnostic.SimpleLoggingDiagnosticListener
import com.squareup.workflow.diagnostic.andThen
import com.squareup.workflow.internal.ZombieWorkflow.ChildSpec
import com.squareup.workflow.internal.ZombieWorkflow.Spec
import com.squareup.workflow.launchWorkflowIn
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateless
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.properties.Delegates.observable
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkflowDiagnosticListenerIntegrationTest {

  private val listener = RecordingDiagnosticListener()

  @UseExperimental(FlowPreview::class)
  @Test fun `workflow tree structure changes emit events in order`() {
    val specChannel = Channel<Spec>()
    var spec by observable<Spec?>(null) { _, _, newValue ->
      newValue?.let { specChannel.offer(it) }
    }

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
      assertEquals("state: root state\n", renderings.receive())
      assertEquals(
          listOf(
              "onWorkflowStarted",
              "onBeforeRenderPass",
              "onPropsChanged",
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
              "onPropsChanged",
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
              "onPropsChanged",
              "onBeforeWorkflowRendered",
              "onAfterWorkflowRendered",
              "onWorkflowStarted",
              "onPropsChanged",
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
              "onPropsChanged",
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

  @UseExperimental(ExperimentalCoroutinesApi::class)
  @Test fun `workflow updates emit events in order`() {
    val channel = Channel<String>()
    val worker = channel.asWorker()
    fun action(value: String) = WorkflowAction<Nothing, String> { "output:$value" }
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(worker, "key", ::action)
    }

    runBlocking {
      launchWorkflowIn(this, workflow, flowOf(Unit)) { session ->
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
              "onPropsChanged",
              "onBeforeWorkflowRendered",
              "onAfterWorkflowRendered",
              "onWorkerStarted",
              "onAfterRenderPass",
              "onBeforeSnapshotPass",
              "onAfterSnapshotPass",
              "onPropsChanged",
              "onBeforeRenderPass",
              "onPropsChanged",
              "onBeforeWorkflowRendered",
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
