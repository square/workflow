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
package com.squareup.workflow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RenderWorkflowInTest {

  // TestCoroutineScope doesn't actually create a Job, so isActive will always return true unless
  // explicitly give it a job.
  private val scope = TestCoroutineScope(Job())

  @Test fun `initial rendering is calculated synchronously`() {
    val props = MutableStateFlow("foo")
    val workflow = Workflow.stateless<String, Nothing, String> { "props: $it" }
    // Don't allow the workflow runtime to actually start.
    scope.pauseDispatcher()
    val renderings = renderWorkflowIn(workflow, scope, props) {}
    assertEquals("props: foo", renderings.value.rendering)
  }

  @Test fun `initial rendering is calculated when scope cancelled before start`() {
    val props = MutableStateFlow("foo")
    val workflow = Workflow.stateless<String, Nothing, String> { "props: $it" }

    scope.cancel()
    val renderings = renderWorkflowIn(workflow, scope, props) {}
    assertEquals("props: foo", renderings.value.rendering)
  }

  @Ignore("https://github.com/square/workflow/issues/1197")
  @Test fun `workers from initial rendering are never started when scope cancelled before start`() {
    var workerWasRan = false
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        workerWasRan = true
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause ->
            cancellationException = cause
          }
        }
      })
    }

    scope.cancel()
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}

    scope.advanceUntilIdle()
    assertFalse(workerWasRan)
    assertNull(cancellationException)
  }

  @Test fun `new renderings are emitted on update`() {
    val props = MutableStateFlow("foo")
    val workflow = Workflow.stateless<String, Nothing, String> { "props: $it" }
    val renderings = renderWorkflowIn(workflow, scope, props) {}

    scope.advanceUntilIdle()
    assertEquals("props: foo", renderings.value.rendering)

    props.value = "bar"
    scope.advanceUntilIdle()
    assertEquals("props: bar", renderings.value.rendering)
  }

  @Test fun `saves to and restores from snapshot`() {
    val workflow = Workflow.stateful<Unit, String, Nothing, Pair<String, (String) -> Unit>>(
        initialState = { _, snapshot ->
          snapshot?.bytes?.parse { it.readUtf8WithLength() } ?: "initial state"
        },
        snapshot = { state ->
          Snapshot.write { it.writeUtf8WithLength(state) }
        },
        render = { _, state ->
          Pair(
              state,
              { newState -> actionSink.send(action { nextState = newState }) }
          )
        }
    )
    val props = MutableStateFlow(Unit)
    val renderings = renderWorkflowIn(workflow, scope, props) {}

    // Interact with the workflow to change the state.
    renderings.value.rendering.let { (state, updateState) ->
      assertEquals("initial state", state)
      updateState("updated state")
    }

    val snapshot = renderings.value.let { (rendering, snapshot) ->
      val (state, updateState) = rendering
      assertEquals("updated state", state)
      updateState("ignored rendering")
      return@let snapshot
    }

    // Create a new scope to launch a second runtime to restore.
    val restoreScope = TestCoroutineScope()
    val restoredRenderings =
      renderWorkflowIn(workflow, restoreScope, props, initialSnapshot = snapshot) {}
    assertEquals("updated state", restoredRenderings.value.rendering.first)
  }

  @Test fun `onOutput called when output emitted`() {
    val trigger = Channel<String>()
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(trigger.asWorker()) { action { setOutput(it) } }
    }
    val receivedOutputs = mutableListOf<String>()
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) { receivedOutputs += it }
    assertTrue(receivedOutputs.isEmpty())

    trigger.offer("foo")
    scope.advanceUntilIdle()
    assertEquals(listOf("foo"), receivedOutputs)

    trigger.offer("bar")
    scope.advanceUntilIdle()
    assertEquals(listOf("foo", "bar"), receivedOutputs)
  }

  /**
   * Since the initial render occurs before launching the coroutine, an exception thrown from it
   * doesn't implicitly cancel the scope. If it did, the reception would be reported twice: once to
   * the caller, and once to the scope.
   */
  @Test fun `exception from initial render doesn't fail parent scope`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      throw ExpectedException()
    }
    scope.pauseDispatcher()
    assertFailsWith<ExpectedException> {
      renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}
    }
    scope.advanceUntilIdle()
    assertTrue(scope.isActive)
  }

  /**
   * The initial render pass can start workers, but if it fails afterwards, those workers should
   * still be cancelled.
   */
  @Test fun `exception from initial render cancels runtime`() {
    var workerWasRan = false
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        workerWasRan = true
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause -> cancellationException = cause }
        }
      })
      throw ExpectedException()
    }

    scope.pauseDispatcher()
    assertFailsWith<ExpectedException> {
      renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}
    }
    assertTrue(workerWasRan)
    assertNotNull(cancellationException)
    val realCause = generateSequence(cancellationException) { it.cause }
        .firstOrNull { it !is CancellationException }
    assertTrue(realCause is ExpectedException)
  }

  @Test fun `exception from non-initial render fails parent scope`() {
    val trigger = CompletableDeferred<Unit>()
    // Throws an exception when trigger is completed.
    val workflow = Workflow.stateful<Unit, Boolean, Nothing, Unit>(
        initialState = { false },
        render = { _, throwNow ->
          runningWorker(trigger.asWorker()) { action { nextState = true } }
          if (throwNow) {
            throw ExpectedException()
          }
        }
    )
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}

    scope.advanceUntilIdle()
    assertTrue(scope.isActive)

    trigger.complete(Unit)
    scope.advanceUntilIdle()
    assertFalse(scope.isActive)
  }

  @Test fun `exception from action fails parent scope`() {
    val trigger = CompletableDeferred<Unit>()
    // Throws an exception when trigger is completed.
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(trigger.asWorker()) {
        action {
          throw ExpectedException()
        }
      }
    }
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}

    scope.advanceUntilIdle()
    assertTrue(scope.isActive)

    trigger.complete(Unit)
    scope.advanceUntilIdle()
    assertFalse(scope.isActive)
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
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}
    assertNull(cancellationException)
    assertTrue(scope.isActive)

    scope.cancel()
    scope.advanceUntilIdle()
    assertTrue(cancellationException is CancellationException)
    assertNull(cancellationException!!.cause)
  }

  @Test fun `failing scope cancels runtime`() {
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause -> cancellationException = cause }
        }
      })
    }
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}
    assertNull(cancellationException)
    assertTrue(scope.isActive)

    scope.cancel(CancellationException("fail!", ExpectedException()))
    scope.advanceUntilIdle()
    assertTrue(cancellationException is CancellationException)
    assertTrue(cancellationException!!.cause is ExpectedException)
  }

  @Test fun `error from renderings collector doesn't fail parent scope`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {}
    val renderings = renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}

    // Collect in separate scope so we actually test that the parent scope is failed when it's
    // different from the collecting scope.
    val collectScope = CoroutineScope(Unconfined)
    collectScope.launch {
      renderings.collect { throw ExpectedException() }
    }

    scope.advanceUntilIdle()
    assertTrue(scope.isActive)
    assertFalse(collectScope.isActive)
  }

  @Test fun `error from renderings collector cancels runtime`() {
    var cancellationException: Throwable? = null
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      runningWorker(Worker.createSideEffect {
        suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cause ->
            cancellationException = cause
          }
        }
      })
    }
    val renderings = renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {}

    scope.pauseDispatcher()
    scope.launch {
      renderings.collect { throw ExpectedException() }
    }
    assertNull(cancellationException)

    scope.advanceUntilIdle()
    assertTrue(cancellationException is CancellationException)
    assertTrue(cancellationException!!.cause is ExpectedException)
  }

  @Test fun `exception from onOutput fails parent scope`() {
    val trigger = CompletableDeferred<Unit>()
    // Emits a Unit when trigger is completed.
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(trigger.asWorker()) { action { setOutput(Unit) } }
    }
    renderWorkflowIn(workflow, scope, MutableStateFlow(Unit)) {
      throw ExpectedException()
    }
    assertTrue(scope.isActive)

    scope.pauseDispatcher()
    trigger.complete(Unit)
    assertTrue(scope.isActive)

    scope.resumeDispatcher()
    scope.advanceUntilIdle()
    assertFalse(scope.isActive)
  }

  private class ExpectedException : RuntimeException()
}
