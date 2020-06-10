/*
 * Copyright 2020 Square Inc.
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

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.runningWorker
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowRunnerTest {

  private val dispatcher = TestCoroutineDispatcher()
  private val scope = CoroutineScope(dispatcher)

  @BeforeTest fun setUp() {
    dispatcher.pauseDispatcher()
  }

  @AfterTest fun tearDown() {
    scope.cancel()
  }

  @Test fun `initial nextRendering() returns initial rendering`() {
    val workflow = Workflow.stateless<Unit, Nothing, String> { "foo" }
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    val rendering = runner.nextRendering().rendering
    assertEquals("foo", rendering)
  }

  @Test fun `initial nextRendering() uses initial props`() {
    val workflow = Workflow.stateless<String, Nothing, String> { it }
    val runner = WorkflowRunner(workflow, MutableStateFlow("foo"))
    val rendering = runner.nextRendering().rendering
    assertEquals("foo", rendering)
  }

  @Test fun `initial nextOutput() does not handle initial props`() {
    val workflow = Workflow.stateless<String, Nothing, String> { it }
    val props = MutableStateFlow("initial")
    val runner = WorkflowRunner(workflow, props)
    runner.nextRendering()
    val output = scope.async { runner.nextOutput() }

    dispatcher.resumeDispatcher()
    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)
  }

  @Test fun `initial nextOutput() handles props changed after initialization`() {
    val workflow = Workflow.stateless<String, Nothing, String> { it }
    val props = MutableStateFlow("initial")
    // The dispatcher is paused, so the produceIn coroutine won't start yet.
    val runner = WorkflowRunner(workflow, props)
    // The initial value will be read during initialization, so we can change it any time after
    // that.
    props.value = "changed"

    // Get the runner into the state where it's waiting for a props update.
    val initialRendering = runner.nextRendering().rendering
    assertEquals("initial", initialRendering)
    val output = scope.async { runner.nextOutput() }
    assertTrue(output.isActive)

    // Resume the dispatcher to start the coroutines and process the new props value.
    dispatcher.resumeDispatcher()

    assertTrue(output.isCompleted)
    assertNull(output.getCompleted())
    val rendering = runner.nextRendering().rendering
    assertEquals("changed", rendering)
  }

  @Test fun `nextOutput() handles workflow update`() {
    val workflow = Workflow.stateful<Unit, String, String, String>(
        initialState = { "initial" },
        render = { _, state ->
          runningWorker(Worker.from { "work" }) {
            action {
              nextState = "state: $it"
              setOutput("output: $it")
            }
          }
          return@stateful state
        }
    )
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    dispatcher.resumeDispatcher()

    val initialRendering = runner.nextRendering().rendering
    assertEquals("initial", initialRendering)

    val output = scope.async { runner.nextOutput() }
        .getCompleted()
    assertEquals("output: work", output)

    val updatedRendering = runner.nextRendering().rendering
    assertEquals("state: work", updatedRendering)
  }

  @Test fun `nextOutput() handles concurrent props change and workflow update`() {
    val workflow = Workflow.stateful<String, String, String, String>(
        initialState = { "initial state($it)" },
        render = { props, state ->
          runningWorker(Worker.from { "work" }) {
            action {
              nextState = "state: $it"
              setOutput("output: $it")
            }
          }
          return@stateful "$props|$state"
        }
    )
    val props = MutableStateFlow("initial props")
    val runner = WorkflowRunner(workflow, props)
    props.value = "changed props"
    val initialRendering = runner.nextRendering().rendering
    assertEquals("initial props|initial state(initial props)", initialRendering)

    dispatcher.resumeDispatcher()

    // The order in which props update and workflow update are processed is deterministic, based
    // on the order they appear in the select block in nextOutput.
    val firstOutput = scope.async { runner.nextOutput() }
        .getCompleted()
    // First update will be props, so no output value.
    assertNull(firstOutput)
    val secondRendering = runner.nextRendering().rendering
    assertEquals("changed props|initial state(initial props)", secondRendering)

    val secondOutput = scope.async { runner.nextOutput() }
        .getCompleted()
    assertEquals("output: work", secondOutput)
    val thirdRendering = runner.nextRendering().rendering
    assertEquals("changed props|state: work", thirdRendering)
  }

  @Test fun `cancelRuntime() does not interrupt nextOutput()`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {}
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    runner.nextRendering()
    val output = scope.async { runner.nextOutput() }
    dispatcher.resumeDispatcher()
    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)

    // nextOutput is run on the scope passed to the runner, so it shouldn't be affected by this
    // call.
    runner.cancelRuntime()

    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)
  }

  @Test fun `cancelRuntime() cancels runtime`() {
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause -> cancellationException = cause }
        }
      })
    }
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    runner.nextRendering()
    val output = scope.async { runner.nextOutput() }
    dispatcher.resumeDispatcher()
    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)
    assertNull(cancellationException)

    runner.cancelRuntime()

    dispatcher.advanceUntilIdle()
    assertTrue(output.isCancelled)
    assertNotNull(cancellationException)
    val causes = generateSequence(cancellationException) { it.cause }
    assertTrue(causes.all { it is CancellationException })
  }

  @Test fun `cancelling scope interrupts nextOutput()`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {}
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    runner.nextRendering()
    val output = scope.async { runner.nextOutput() }
    dispatcher.resumeDispatcher()
    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)

    scope.cancel("foo")

    dispatcher.advanceUntilIdle()
    assertTrue(output.isCancelled)
    val realCause = output.getCompletionExceptionOrNull()
    assertEquals("foo", realCause?.message)
  }

  @Test fun `cancelling scope cancels runtime`() {
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause -> cancellationException = cause }
        }
      })
    }
    val runner = WorkflowRunner(workflow, MutableStateFlow(Unit))
    runner.nextRendering()
    val output = scope.async { runner.nextOutput() }
    dispatcher.resumeDispatcher()
    dispatcher.advanceUntilIdle()
    assertTrue(output.isActive)
    assertNull(cancellationException)

    scope.cancel("foo")

    dispatcher.advanceUntilIdle()
    assertTrue(output.isCancelled)
    assertNotNull(cancellationException)
    assertEquals("foo", cancellationException!!.message)
  }

  @Suppress("TestFunctionName")
  private fun <P, O : Any, R> WorkflowRunner(
    workflow: Workflow<P, O, R>,
    props: StateFlow<P>
  ) =
    WorkflowRunner(scope, workflow, props, null, null)
}
