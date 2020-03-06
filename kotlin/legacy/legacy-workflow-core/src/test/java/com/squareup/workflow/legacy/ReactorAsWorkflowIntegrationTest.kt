/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy

import com.squareup.workflow.legacy.TestState.FirstState
import com.squareup.workflow.legacy.TestState.SecondState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ReactorAsWorkflowIntegrationTest {

  private open class MockReactor : Reactor<TestState, Nothing, String> {
    override suspend fun onReact(
      state: TestState,
      events: ReceiveChannel<Nothing>,
      workflows: WorkflowPool
    ): Reaction<TestState, String> = suspendCancellableCoroutine {
      // Suspend forever.
    }
  }

  private lateinit var stateSub: ReceiveChannel<TestState>
  private lateinit var resultSub: Deferred<String>

  private var reactor: Reactor<TestState, Nothing, String> = MockReactor()
  private lateinit var workflow: Workflow<TestState, Nothing, String>
  private val uncaughtExceptions = mutableListOf<Throwable>()
  private val workflowContext = CoroutineExceptionHandler { _, e -> uncaughtExceptions += e }

  private fun start(input: String) {
    workflow = reactor.doLaunch(FirstState(input), WorkflowPool(), workflowContext)
        .apply {
          subscribeToState(this)
          subscribeToResult(this)
        }
  }

  private fun subscribeToState(workflow: Workflow<TestState, *, *>) {
    stateSub = workflow.openSubscriptionToState()
  }

  private fun subscribeToResult(workflow: Workflow<*, *, String>) {
    resultSub = GlobalScope.async(Unconfined) { workflow.await() }
  }

  @Test fun `start new initial state after start new`() {
    start("hello")

    assertEquals(FirstState("hello"), stateSub.poll())
    assertTrue(stateSub.isEmpty)
    assertFalse(stateSub.isClosedForReceive)
    assertFalse(resultSub.isCompleted)
  }

  @Test fun `start from state`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())

    subscribeToState(workflow)
    subscribeToResult(workflow)

    assertEquals(SecondState("hello"), stateSub.poll())
    assertTrue(stateSub.isEmpty)
    assertFalse(stateSub.isClosedForReceive)
    assertFalse(resultSub.isCompleted)
  }

  @Test fun `state completes when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())

    subscribeToState(workflow)
    workflow.cancel()

    assertEquals(SecondState("hello"), stateSub.poll())
    assertFailsWith<CancellationException> { stateSub.poll() }
    assertTrue(stateSub.isClosedForReceive)
  }

  @Test fun `state stays completed for late subscribers when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())
    workflow.cancel()

    subscribeToState(workflow)
    assertTrue(stateSub.isClosedForReceive)
  }

  @Test fun `result completes when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())
    subscribeToResult(workflow)
    workflow.cancel()
    assertTrue(resultSub.isCancelled && resultSub.isCompleted)
  }

  @Test fun `single state change and then finish`() {
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        return when (state) {
          is FirstState -> EnterState(SecondState("${state.value} ${state.value}"))
          is SecondState -> FinishWith("all done")
        }
      }
    }

    start("hello")
    // We never see the values b/c the machine races through its states and finishes
    // the instant we start it.
    assertTrue(stateSub.isClosedForReceive)
    assertEquals("all done", resultSub.getCompleted())
  }

  @Test fun `delayed state change`() {
    val secondStateDeferred = CompletableDeferred<TestState>()

    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        return when (state) {
          is FirstState -> EnterState(secondStateDeferred.await())
          is SecondState -> FinishWith("all done")
        }
      }
    }

    start("hello")

    assertEquals(FirstState("hello"), stateSub.poll())
    assertTrue(stateSub.isEmpty)
    assertFalse(resultSub.isCompleted)

    secondStateDeferred.complete(SecondState("foo"))
    assertEquals(SecondState("foo"), stateSub.poll())
    assertTrue(stateSub.isClosedForReceive)
    assertEquals("all done", resultSub.getCompleted())
  }

  @Test fun `when react throws immediately`() {
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        throw RuntimeException("((angery))")
      }
    }

    start("foo")

    try {
      stateSub.poll()
      fail("Expected exception")
    } catch (e: ReactorException) {
      assertTrue("threw RuntimeException: ((angery))" in e.message)
    }

    try {
      resultSub.getCompleted()
      fail("Expected exception")
    } catch (e: ReactorException) {
      assertTrue("threw RuntimeException: ((angery))" in e.message)
    }
  }

  @Test fun `when react throws after suspending`() {
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        suspendCoroutine<Nothing> { continuation ->
          continuation.resumeWithException(RuntimeException("((angery))"))
        }
      }
    }

    start("foo")

    try {
      stateSub.poll()
      fail("Expected exception")
    } catch (e: ReactorException) {
      assertTrue("threw RuntimeException: ((angery))" in e.message)
    }

    try {
      resultSub.getCompleted()
      fail("Expected exception")
    } catch (e: ReactorException) {
      assertTrue("threw RuntimeException: ((angery))" in e.message)
    }
  }

  @Test fun `react is cancelled on abandonment`() {
    var invoked = false
    var cancelled = false
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        invoked = true
        return suspendCancellableCoroutine { continuation ->
          continuation.invokeOnCancellation { cancelled = true }
        }
      }
    }

    start("foo")

    assertTrue(invoked)
    assertFalse(cancelled)

    workflow.cancel()

    assertTrue(invoked)
    assertTrue(cancelled)
  }

  @Test
  fun `exception is propagated when state subscriber throws from second onNext asynchronously`() {
    val trigger = CompletableDeferred<Unit>()
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> =
        when (state) {
          is FirstState -> {
            trigger.await()
            EnterState(SecondState(""))
          }
          is SecondState -> super.onReact(state, events, workflows)
        }
    }

    start("foo")
    stateSub.cancel()

    GlobalScope.launch(Unconfined) {
      workflow.openSubscriptionToState()
          .consumeEach { /* No-op */ }
    }

    val uncaughtExceptions = mutableListOf<Throwable>()
    GlobalScope.launch(Unconfined + CoroutineExceptionHandler { _, throwable ->
      uncaughtExceptions += throwable
    }) {
      workflow.openSubscriptionToState()
          .consumeEach { state ->
            if (state is SecondState) throw RuntimeException("fail")
          }
    }

    assertTrue(uncaughtExceptions.isEmpty())

    trigger.complete(Unit)

    assertEquals(1, uncaughtExceptions.size)
    assertTrue {
      uncaughtExceptions[0] is RuntimeException &&
          uncaughtExceptions[0].message == "fail"
    }
  }

  @Test fun `accepts events before subscriptions`() {
    val reactor = object : Reactor<FirstState, String, String> {
      override suspend fun onReact(
        state: FirstState,
        events: ReceiveChannel<String>,
        workflows: WorkflowPool
      ): Reaction<FirstState, String> {
        val event = events.receive()
        return EnterState(FirstState(event))
      }
    }

    // Unused, but just to be certain it doesn't get gc'd on us. Silly, I know.
    val workflow = reactor.doLaunch(FirstState("Hello"), WorkflowPool())
    workflow.sendEvent("Fnord")
    // No crash, no bug!
  }

  @Test fun `buffers events when select not called`() {
    val proceedToSecondState = CompletableDeferred<Unit>()
    val reactor = object : Reactor<TestState, String, String> {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<String>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> = when (state) {
        is FirstState -> {
          proceedToSecondState.await()
          EnterState(SecondState(""))
        }
        is SecondState -> FinishWith(events.receive())
      }
    }
    val workflow = reactor.doLaunch(FirstState("Hello"), WorkflowPool())
    subscribeToResult(workflow)

    // This event should get buffered…
    workflow.sendEvent("Fnord")

    // …but not accepted yet.
    assertFalse(resultSub.isCompleted)

    // This triggers the events.select, which should finish the workflow.
    proceedToSecondState.complete(Unit)
    assertEquals("Fnord", resultSub.getCompleted())
  }

  @Test fun `buffers events when reentrant`() {
    lateinit var workflow: Workflow<TestState, String, String>
    val reactor = object : Reactor<TestState, String, String> {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<String>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> = when (state) {
        is FirstState -> {
          val event = events.receive()
          workflow.sendEvent("i heard you like events")
          EnterState(SecondState(event))
        }
        is SecondState -> FinishWith(events.receive())
      }
    }
    workflow = reactor.doLaunch(FirstState(""), WorkflowPool())
    subscribeToState(workflow)
    subscribeToResult(workflow)

    assertEquals(FirstState(""), stateSub.poll())
    assertFalse(resultSub.isCompleted)

    workflow.sendEvent("foo")
    assertEquals(SecondState("foo"), stateSub.poll())
    assertTrue(stateSub.isClosedForReceive)
    assertEquals("i heard you like events", resultSub.getCompleted())
  }

  @Test fun `state receiveOrNull returns null onFinishImmediately`() {
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> = FinishWith("done")
    }
    start("foo")

    runBlocking {
      assertEquals(null, stateSub.receiveOrNull())
    }
  }

  @Test fun `state receiveOrNull returns null onFinishLater`() {
    val trigger = CompletableDeferred<Unit>()
    reactor = object : MockReactor() {
      override suspend fun onReact(
        state: TestState,
        events: ReceiveChannel<Nothing>,
        workflows: WorkflowPool
      ): Reaction<TestState, String> {
        trigger.await()
        return FinishWith("done")
      }
    }
    start("foo")
    assertEquals(FirstState("foo"), stateSub.poll())
    trigger.complete(Unit)

    runBlocking {
      assertEquals(null, stateSub.receiveOrNull())
    }
  }
}

private sealed class TestState {
  data class FirstState(val value: String) : TestState()
  data class SecondState(val value: String) : TestState()
}
