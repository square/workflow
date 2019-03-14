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

import com.squareup.workflow.legacy.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.SubtreeManagerTest.TestWorkflow.Rendering
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

class SubtreeManagerTest {

  private class TestWorkflow : Workflow<String, String, String, Rendering> {

    var started = 0

    data class Rendering(
      val input: String,
      val state: String,
      val eventHandler: (String) -> Unit
    )

    override fun initialState(input: String): String {
      started++
      return "initialState:$input"
    }

    override fun compose(
      input: String,
      state: String,
      context: WorkflowContext<String, String>
    ): Rendering = Rendering(input, state, context.makeSink {
      emitOutput("workflow output:$it")
    })

    override fun snapshotState(state: String) = fail()
    override fun restoreState(snapshot: Snapshot) = fail()
  }

  private val context = Dispatchers.Unconfined

  @Test fun compose_startsNewChild() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    manager.compose(case, workflow, id, input)
    assertEquals(1, workflow.started)
  }

  @Test fun compose_doesntStartExistingChild() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    manager.compose(case, workflow, id, input)
    manager.compose(case, workflow, id, input)
    assertEquals(1, workflow.started)
  }

  @Test fun compose_returnsChildRendering() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) { fail() }

    val (composeInput, composeState) = manager.compose(case, workflow, id, input)
    assertEquals("input", composeInput)
    assertEquals("initialState:input", composeState)
  }

  @Test fun tickChildren_handlesChildOutput() {
    val manager = SubtreeManager<String, String>(context)
    val workflow = TestWorkflow()
    val id = workflow.id()
    val input = "input"
    val case = WorkflowOutputCase<String, String, String, String>(workflow, id, input) {
      emitOutput("case output:$it")
    }

    // Initialize the child so tickChildren has something to work with, and so that we can send
    // an event to trigger an output.
    val (_, _, eventHandler) = manager.compose(case, workflow, id, "input")

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
      val (_, output) = update.invoke("state")
      assertEquals("case output:workflow output:event!", output)
    }
  }
}
