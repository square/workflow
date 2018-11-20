package com.squareup.reactor

import com.squareup.reactor.TestState.FirstState
import com.squareup.reactor.TestState.SecondState
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ReactorAsWorkflowIntegrationTest {

  private open class MockReactor : Reactor<TestState, Nothing, String> {
    override suspend fun react(
      state: TestState,
      events: ReceiveChannel<Nothing>
    ): Reaction<TestState, String> = suspendCancellableCoroutine { }
  }

  private lateinit var stateSub: ReceiveChannel<TestState>
  private lateinit var resultSub: Deferred<String>

  private var reactor: Reactor<TestState, Nothing, String> = MockReactor()
  private lateinit var workflow: Workflow<TestState, Nothing, String>

  @Suppress("UNCHECKED_CAST")
  fun start(input: String) {
    workflow = reactor.startWorkflow(FirstState(input))
        .apply {
          subscribeToState(this)
          subscribeToResult(this)
        }
  }

  private fun subscribeToState(workflow: Workflow<TestState, *, *>) {
    // Buffer the outputs so we can see what was emitted before the workflow finished.
    stateSub = GlobalScope.produce(Unconfined, capacity = UNLIMITED) {
      workflow.openSubscriptionToState()
          .consumeEach { send(it) }
    }
  }

  private fun subscribeToResult(workflow: Workflow<*, *, String>) {
    resultSub = GlobalScope.async(Unconfined) { workflow.awaitResult() }
  }

  @Test fun startNew_initialState_afterStartNew() {
    start("hello")

    assertEquals(FirstState("hello"), stateSub.poll())
    assertTrue(stateSub.isEmpty)
    assertFalse(stateSub.isClosedForReceive)
    assertFalse(resultSub.isCompleted)
  }

  @Test fun startFromState() {
    val workflow = reactor.startWorkflow(SecondState("hello"))

    subscribeToState(workflow)
    subscribeToResult(workflow)

    assertEquals(SecondState("hello"), stateSub.poll())
    assertTrue(stateSub.isEmpty)
    assertFalse(stateSub.isClosedForReceive)
    assertFalse(resultSub.isCompleted)
  }

  @Test fun stateCompletesWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))

    subscribeToState(workflow)
    workflow.abandon()

    assertEquals(SecondState("hello"), stateSub.poll())
    assertTrue(stateSub.isClosedForReceive)
  }

  @Test fun stateStaysCompletedForLateSubscribersWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))
    workflow.abandon()

    subscribeToState(workflow)
    assertTrue(stateSub.isClosedForReceive)
  }

  @Test fun resultCompletesWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))
    subscribeToResult(workflow)
    workflow.abandon()
    assertTrue(resultSub.isCompletedExceptionally)
  }

  @Test fun singleStateChangeAndThenFinish() {
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
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

  @Test fun delayedStateChange() {
    val secondStateDeferred = CompletableDeferred<TestState>()

    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
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
  }

  @Test fun whenReactThrowsDirectly() {
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
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

  @Test fun whenReactSingleThrows() {
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
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

  @Test fun reactIsCancelled_onAbandonment() {
    var invoked = false
    var cancelled = false
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
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

    workflow.abandon()

    assertTrue(invoked)
    assertTrue(cancelled)
  }

  @Test fun reactorIsNotAbandoned_whenInFinishedState() {
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
      ): Reaction<TestState, String> {
        return FinishWith("all done")
      }

      override fun abandon(state: TestState) {
        fail("Expected onAbandoned not to be called.")
      }
    }

    start("starting")
    workflow.abandon()
  }

  @Test fun reactorIsAbandoned_onAbandonment() {
    val log = StringBuilder()

    reactor = object : MockReactor() {
      override fun abandon(state: TestState) {
        log.append("+reactor.onAbandoned")
      }
    }

    start("starting")
    GlobalScope.launch(Unconfined) {
      try {
        workflow.openSubscriptionToState()
            .consumeEach { }
      } finally {
        // This needs to be in the finally since the channel will be canceled.
        log.append("+workflow.Completed")
      }
    }
    workflow.abandon()

    // Docs promise that the workflow doesn't complete until the reactor has a chance
    // to do its abandon thing.
    assertEquals("+reactor.onAbandoned+workflow.Completed", log.toString())
  }

  @Test fun exceptionIsPropagated_whenStateSubscriberThrowsFromSecondOnNext_asynchronously() {
    val trigger = CompletableDeferred<Unit>()
    reactor = object : MockReactor() {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<Nothing>
      ): Reaction<TestState, String> =
        when (state) {
          is FirstState -> {
            trigger.await()
            EnterState(SecondState(""))
          }
          is SecondState -> super.react(state, events)
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

  @Test fun acceptsEventsBeforeSubscriptions() {
    val reactor = object : Reactor<FirstState, String, String> {
      override suspend fun react(
        state: FirstState,
        events: ReceiveChannel<String>
      ): Reaction<FirstState, String> {
        val event = events.receive()
        return EnterState(FirstState(event))
      }
    }

    // Unused, but just to be certain it doesn't get gc'd on us. Silly, I know.
    val workflow = reactor.startWorkflow(FirstState("Hello"))
    workflow.sendEvent("Fnord")
    // No crash, no bug!
  }

  @Test fun buffersEvents_whenSelectNotCalled() {
    val proceedToSecondState = CompletableDeferred<Unit>()
    val reactor = object : Reactor<TestState, String, String> {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<String>
      ): Reaction<TestState, String> = when (state) {
        is FirstState -> {
          proceedToSecondState.await()
          EnterState(SecondState(""))
        }
        is SecondState -> FinishWith(events.receive())
      }
    }
    val workflow = reactor.startWorkflow(FirstState("Hello"))
    subscribeToResult(workflow)

    // This event should get buffered…
    workflow.sendEvent("Fnord")

    // …but not accepted yet.
    assertFalse(resultSub.isCompleted)

    // This triggers the events.select, which should finish the workflow.
    proceedToSecondState.complete(Unit)
    assertEquals("Fnord", resultSub.getCompleted())
  }

  @Test fun buffersEvents_whenReentrant() {
    lateinit var workflow: Workflow<TestState, String, String>
    val reactor = object : Reactor<TestState, String, String> {
      override suspend fun react(
        state: TestState,
        events: ReceiveChannel<String>
      ): Reaction<TestState, String> = when (state) {
        is FirstState -> {
          val event = events.receive()
          workflow.sendEvent("i heard you like events")
          EnterState(SecondState(event))
        }
        is SecondState -> FinishWith(events.receive())
      }
    }
    workflow = reactor.startWorkflow(FirstState(""))
    subscribeToState(workflow)
    subscribeToResult(workflow)

    assertEquals(FirstState(""), stateSub.poll())
    assertFalse(resultSub.isCompleted)

    workflow.sendEvent("foo")
    assertEquals(SecondState("foo"), stateSub.poll())
    assertEquals("i heard you like events", resultSub.getCompleted())
  }
}

private sealed class TestState {
  data class FirstState(val value: String) : TestState()
  data class SecondState(val value: String) : TestState()
}
