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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.testing

import com.squareup.workflow.Sink
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.makeEventSink
import com.squareup.workflow.renderChild
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DeprecatedRenderTesterTest {

  @Test fun `stateless props and rendering`() {
    val workflow = Workflow.stateless<String, String, String> { props ->
      return@stateless "props: $props"
    } as StatelessWorkflow

    workflow.testRender("start") {
      assertEquals("props: start", rendering)
    }
  }

  @Test fun `stateful workflow gets state`() {
    val workflow = Workflow.stateful<String, String, Nothing, String>(
        initialState = { _, _ -> fail("Expected initialState not to be called.") },
        render = { props, state -> "props=$props, state=$state" },
        snapshot = { fail("Expected snapshotState not to be called.") }
    )

    workflow.testRender(props = "foo", state = "bar") {
      assertEquals("props=foo, state=bar", rendering)
    }
  }

  @Test fun `testRenderInitialState uses correct state`() {
    val workflow = Workflow.stateful<String, String, String, String>(
        initialState = { props, _ -> props },
        render = { props, state -> "props: $props, state: $state" },
        snapshot = { fail() }
    )

    workflow.testRenderInitialState("initial") {
      assertEquals("props: initial, state: initial", rendering)
    }
  }

  @Test fun `assert no composition`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { Unit } as StatelessWorkflow

    workflow.testRender {
      assertNoWorkflowsRendered()
      assertNoWorkersRan()
    }
  }

  @Test fun `renders child with props`() {
    val child = MockChildWorkflow<String, String> { "props: $it" }
    val workflow = Workflow.stateless<Unit, Nothing, String> {
      "child: " + renderChild(child, "foo")
    } as StatelessWorkflow

    workflow.testRender {
      assertEquals("foo", child.lastSeenProps)
      assertEquals("child: props: foo", rendering)
    }
  }

  @Test fun `renders worker output`() {
    val worker = MockWorker<String>("worker")
    val workflow = Workflow.stateful<Unit, String, String, Unit>(
        initialState = { _, _ -> fail() },
        render = { _, _ ->
          runningWorker(worker) {
            WorkflowAction {
              state = "state: $it"
              "output: $it"
            }
          }
        },
        snapshot = { fail() }
    )

    workflow.testRender("") {
      assertNoWorkflowsRendered()
      worker.assertRan()

      val (outputState, output) = worker.handleOutput("work!")
      assertEquals("state: work!", outputState)
      assertEquals("output: work!", output)
    }
  }

  @Test fun `child workflow output`() {
    val child: Workflow<Unit, String, Unit> = MockChildWorkflow(Unit)
    val workflow = Workflow.stateful<Unit, String, String, Unit>(
        initialState = { _, _ -> fail() },
        render = { _, _ ->
          renderChild(child) {
            WorkflowAction {
              state = "state: $it"
              "output: $it"
            }
          }
        },
        snapshot = { fail() }
    )

    workflow.testRender("") {
      assertNoWorkersRan()
      child.assertRendered()
      val (state, output) = child.handleOutput("output!")
      assertEquals("state: output!", state)
      assertEquals("output: output!", output)
    }
  }

  @Test fun `getEventResult works`() {
    val workflow = Workflow.stateful<Unit, String, String, Sink<String>>(
        initialState = { _, _ -> fail() },
        render = { _, state ->
          makeEventSink { event ->
            this@makeEventSink.state = "from $state on $event"
            "event: $event"
          }
        },
        snapshot = { fail() }
    )

    workflow.testRender(state = "initial") {
      rendering.send("foo")

      val (state, output) = getEventResult()
      assertEquals("from initial on foo", state)
      assertEquals("event: foo", output)
    }
  }

  @Test fun `detects render side effects`() {
    var renderCount = 0
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderCount++
    } as StatelessWorkflow

    workflow.testRender {
      assertEquals(2, renderCount)
    }
  }
}
