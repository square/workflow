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
package com.squareup.workflow.testing

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateless
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RenderTesterTest {

  @Test fun `stateless input and rendering`() {
    val workflow = Workflow.stateless<String, String, String> { input, context ->
      return@stateless "input: $input"
    } as StatelessWorkflow

    val result = workflow.testRender("start")

    assertEquals("input: start", result.rendering)
  }

  @Test fun `stateful workflow gets state`() {
    val workflow = object : StatefulWorkflow<String, String, Nothing, String>() {
      override fun initialState(
        input: String,
        snapshot: Snapshot?
      ): String = fail("Expected initialState not to be called.")

      override fun render(
        input: String,
        state: String,
        context: RenderContext<String, Nothing>
      ): String {
        return "input=$input, state=$state"
      }

      override fun snapshotState(state: String): Snapshot =
        fail("Expected snapshotState not to be called.")
    }

    val result = workflow.testRender(input = "foo", state = "bar")

    assertEquals("input=foo, state=bar", result.rendering)
  }

  @Test fun `assert no composition`() {
    val workflow = Workflow.stateless<Nothing, Unit> { Unit } as StatelessWorkflow

    val result = workflow.testRender()

    result.assertNoWorkflowsRendered()
    result.assertNoWorkersRan()
  }

  @Test fun `renders child with input`() {
    val child = MockChildWorkflow<String, String> { "input: $it" }
    val workflow = Workflow.stateless<Nothing, String> { context ->
      "child: " + context.renderChild(child, "foo")
    } as StatelessWorkflow

    val result = workflow.testRender()

    assertEquals("foo", child.lastSeenInput)
    assertEquals("child: input: foo", result.rendering)
  }

  @Test fun `renders worker output`() {
    val worker = MockWorker<String>("worker")
    val workflow = object : StatefulWorkflow<Unit, String, String, Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?
      ): String = fail()

      override fun render(
        input: Unit,
        state: String,
        context: RenderContext<String, String>
      ) {
        context.onWorkerOutputOrFinished(worker) {
          enterState(
              "state: $it",
              emittingOutput = "output: $it"
          )
        }
      }

      override fun snapshotState(state: String): Snapshot = fail()
    }

    val result = workflow.testRender("")

    result.assertNoWorkflowsRendered()
    result.assertWorkerRan(worker)

    // Output
    val (outputState, output) = result.executeWorkerActionFromOutput(worker, "work!")
    assertEquals("state: Output(value=work!)", outputState)
    assertEquals("output: Output(value=work!)", output)

    // Finish
    val (finishState, finish) = result.executeWorkerActionFromFinish(worker)
    assertEquals("state: Finished", finishState)
    assertEquals("output: Finished", finish)
  }

  @Test fun `child workflow output`() {
    val child: Workflow<Unit, String, Unit> = MockChildWorkflow(Unit)
    val workflow = object : StatefulWorkflow<Unit, String, String, Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?
      ): String = fail()

      override fun render(
        input: Unit,
        state: String,
        context: RenderContext<String, String>
      ) {
        context.renderChild(child) {
          enterState(
              "state: $it",
              emittingOutput = "output: $it"
          )
        }
      }

      override fun snapshotState(state: String): Snapshot = fail()
    }

    val result = workflow.testRender("")

    result.assertNoWorkersRan()
    result.assertWorkflowRendered(child)
    val (state, output) = result.executeWorkflowActionFromOutput(child, "output!")
    assertEquals("state: output!", state)
    assertEquals("output: output!", output)
  }
}
