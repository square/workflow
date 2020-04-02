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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.internal

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.action
import com.squareup.workflow.applyTo
import com.squareup.workflow.internal.SubtreeManagerTest.TestWorkflow.Rendering
import com.squareup.workflow.makeEventSink
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.fail

private typealias StringHandler = (String) -> WorkflowAction<String, String>

class SubtreeManagerTest {

  private class TestWorkflow : StatefulWorkflow<String, String, String, Rendering>() {

    var started = 0

    data class Rendering(
      val props: String,
      val state: String,
      val eventHandler: (String) -> Unit
    )

    override fun initialState(
      props: String,
      snapshot: Snapshot?
    ): String {
      started++
      return "initialState:$props"
    }

    override fun render(
      props: String,
      state: String,
      context: RenderContext<String, String>
    ): Rendering {
      val sink: Sink<String> = context.makeEventSink { setOutput(it) }
      return Rendering(props, state) { sink.send("workflow output:$it") }
    }

    override fun snapshotState(state: String) = fail()
  }

  private class SnapshotTestWorkflow : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {

    var snapshots = 0
    var serializes = 0

    override fun initialState(
      props: Unit,
      snapshot: Snapshot?
    ) {
    }

    override fun render(
      props: Unit,
      state: Unit,
      context: RenderContext<Unit, Nothing>
    ) {
    }

    override fun snapshotState(state: Unit): Snapshot {
      snapshots++
      return Snapshot.write {
        serializes++
      }
    }
  }

  private val context = Unconfined

  @Test fun `render starts new child`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()

    manager.render(workflow, "props", key = "", handler = { fail() })
    assertEquals(1, workflow.started)
  }

  @Test fun `render doesn't start existing child`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()
    fun render() = manager.render(workflow, "props", key = "", handler = { fail() })
        .also { manager.commitRenderedChildren() }

    render()
    render()

    assertEquals(1, workflow.started)
  }

  @Test fun `render restarts child after tearing down`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()
    fun render() = manager.render(workflow, "props", key = "", handler = { fail() })
        .also { manager.commitRenderedChildren() }
    render()
    assertEquals(1, workflow.started)

    // Render without rendering child.
    manager.commitRenderedChildren()
    assertEquals(1, workflow.started)

    render()
    assertEquals(2, workflow.started)
  }

  @Test fun `render throws on duplicate key`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()
    manager.render(workflow, "props", "foo", handler = { fail() })

    val error = assertFailsWith<IllegalArgumentException> {
      manager.render(workflow, "props", "foo", handler = { fail() })
    }
    assertEquals(
        "Expected keys to be unique for ${TestWorkflow::class.java.name}: key=foo",
        error.message
    )
  }

  @Test fun `render returns child rendering`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()

    val (composeProps, composeState) = manager.render(
        workflow, "props", key = "", handler = { fail() }
    )
    assertEquals("props", composeProps)
    assertEquals("initialState:props", composeState)
  }

  @Test fun `tick children handles child output`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()
    val handler: StringHandler = { output ->
      action { setOutput("case output:$output") }
    }

    // Initialize the child so tickChildren has something to work with, and so that we can send
    // an event to trigger an output.
    val (_, _, eventHandler) = manager.render(workflow, "props", key = "", handler = handler)
    manager.commitRenderedChildren()

    runBlocking {
      val tickOutput = async { manager.tickAction() }
      assertFalse(tickOutput.isCompleted)

      eventHandler("event!")
      val update = tickOutput.await()!!
      val (_, output) = update.applyTo("state")
      assertEquals("case output:workflow output:event!", output)
    }
  }

  @Test fun `render updates child's output handler`() {
    val manager = subtreeManagerForTest<String, String>()
    val workflow = TestWorkflow()
    fun render(handler: StringHandler) =
      manager.render(workflow, "props", key = "", handler = handler)
          .also { manager.commitRenderedChildren() }

    runBlocking {
      // First render + tick pass – uninteresting.
      render { action { setOutput("initial handler: $it") } }
          .let { rendering ->
            rendering.eventHandler("initial output")
            val initialAction = manager.tickAction()!!
            val (_, initialOutput) = initialAction.applyTo("")
            assertEquals("initial handler: workflow output:initial output", initialOutput)
          }

      // Do a second render + tick, but with a different handler function.
      render { action { setOutput("second handler: $it") } }
          .let { rendering ->
            rendering.eventHandler("second output")
            val secondAction = manager.tickAction()!!
            val (_, secondOutput) = secondAction.applyTo("")
            assertEquals("second handler: workflow output:second output", secondOutput)
          }
    }
  }

  // See https://github.com/square/workflow/issues/404
  @Test fun `createChildSnapshot snapshots eagerly`() {
    val manager = subtreeManagerForTest<Unit, Nothing>()
    val workflow = SnapshotTestWorkflow()
    assertEquals(0, workflow.snapshots)

    manager.render(workflow, props = Unit, key = "1", handler = { fail() })
    manager.commitRenderedChildren()
    manager.createChildSnapshots()

    assertEquals(1, workflow.snapshots)
  }

  // See https://github.com/square/workflow/issues/404
  @Test fun `createChildSnapshot serializes lazily`() {
    val manager = subtreeManagerForTest<Unit, Nothing>()
    val workflow = SnapshotTestWorkflow()
    assertEquals(0, workflow.serializes)

    manager.render(workflow, props = Unit, key = "1", handler = { fail() })
    manager.commitRenderedChildren()
    val snapshots = manager.createChildSnapshots()

    assertEquals(0, workflow.serializes)

    snapshots.forEach { (_, snapshot) -> snapshot.bytes }
    assertEquals(1, workflow.serializes)
  }

  private suspend fun <S, O : Any> SubtreeManager<S, O>.tickAction(): WorkflowAction<S, O>? =
    select { tickChildren(this) }

  private fun <S, O : Any> subtreeManagerForTest() =
    SubtreeManager<S, O>(context, emitActionToParent = { it }, parentDiagnosticId = 0)
}
