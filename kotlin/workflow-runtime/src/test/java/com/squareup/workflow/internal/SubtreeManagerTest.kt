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
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.applyTo
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.SubtreeManagerTest.TestWorkflow.Rendering
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

class SubtreeManagerTest {

  private class TestWorkflow : StatefulWorkflow<String, String, String, Rendering>() {

    var started = 0

    data class Rendering(
      val input: String,
      val state: String,
      val eventHandler: (String) -> Unit
    )

    override fun initialState(
      input: String,
      snapshot: Snapshot?
    ): String {
      started++
      return "initialState:$input"
    }

    override fun render(
      input: String,
      state: String,
      context: RenderContext<String, String>
    ): Rendering = Rendering(input, state, context.onEvent {
      emitOutput("workflow output:$it")
    })

    override fun snapshotState(state: String) = fail()
  }

  private class SnapshotTestWorkflow : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {

    var snapshots = 0
    var serializes = 0

    override fun initialState(
      input: Unit,
      snapshot: Snapshot?
    ) {
    }

    override fun render(
      input: Unit,
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
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    manager.render(case, workflow, id, input)
    assertEquals(1, workflow.started)
  }

  @Test fun `render doesn't start existing child`() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    manager.render(case, workflow, id, input)
    manager.render(case, workflow, id, input)
    assertEquals(1, workflow.started)
  }

  @Test fun `render returns child rendering`() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    val (composeInput, composeState) = manager.render(case, workflow, id, input)
    assertEquals("input", composeInput)
    assertEquals("initialState:input", composeState)
  }

  @Test fun `tick children handles child output`() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) {
      emitOutput("case output:$it")
    }

    // Initialize the child so tickChildren has something to work with, and so that we can send
    // an event to trigger an output.
    val (_, _, eventHandler) = manager.render(case, workflow, id, "input")

    runBlocking {
      val tickOutput = async {
        select<WorkflowAction<String, String>?> {
          manager.tickChildren(this) { update ->
            return@tickChildren update
          }
        }
      }
      assertFalse(tickOutput.isCompleted)

      eventHandler("event!")
      val update = tickOutput.await()!!
      val (_, output) = update.applyTo("state")
      assertEquals("case output:workflow output:event!", output)
    }
  }

  // See https://github.com/square/workflow/issues/404
  @Test fun `createChildSnapshot snapshots eagerly`() {
    val manager = SubtreeManager<Unit, Nothing>(Unconfined)
    val workflow = SnapshotTestWorkflow()
    val id = workflow.id("1")
    val case = WorkflowOutputCase<Unit, Unit, Unit, Nothing>(workflow, id, Unit) { fail() }
    assertEquals(0, workflow.snapshots)

    manager.track(listOf(case))
    manager.createChildrenSnapshot()
    assertEquals(1, workflow.snapshots)
  }

  // See https://github.com/square/workflow/issues/404
  @Test fun `createChildSnapshot serializes lazily`() {
    val manager = SubtreeManager<Unit, Nothing>(Unconfined)
    val workflow = SnapshotTestWorkflow()
    val id = workflow.id("1")
    val case = WorkflowOutputCase<Unit, Unit, Unit, Nothing>(workflow, id, Unit) { fail() }
    assertEquals(0, workflow.serializes)

    manager.track(listOf(case))
    val snapshot = manager.createChildrenSnapshot()
    assertEquals(0, workflow.serializes)

    snapshot.bytes
    assertEquals(1, workflow.serializes)
  }
}
