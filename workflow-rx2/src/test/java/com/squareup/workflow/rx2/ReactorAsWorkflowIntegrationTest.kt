@file:Suppress("DEPRECATION")

package com.squareup.workflow.rx2

import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.ReactorException
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.rx2.TestState.FirstState
import com.squareup.workflow.rx2.TestState.SecondState
import io.reactivex.Single
import io.reactivex.Single.just
import io.reactivex.Single.never
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ReactorAsWorkflowIntegrationTest {

  private open class MockReactor : Reactor<TestState, Nothing, String> {
    override fun onReact(
      state: TestState,
      events: EventChannel<Nothing>,
      workflows: WorkflowPool
    ): Single<out Reaction<TestState, String>> = never()
  }

  @JvmField @Rule val thrown = ExpectedException.none()!!

  private val stateSub = TestObserver<TestState>()
  private val resultSub = TestObserver<String>()

  private var reactor: Reactor<TestState, Nothing, String> = MockReactor()
  private lateinit var workflow: Workflow<TestState, Nothing, String>

  @Suppress("UNCHECKED_CAST")
  fun start(input: String) {
    workflow = reactor.startRootWorkflow(FirstState(input))
        .apply {
          state.subscribe(stateSub)
          result.subscribe(resultSub)
        }
  }

  @Test fun startNew_initialState_afterStartNew() {
    start("hello")
    stateSub.assertValue(FirstState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()
  }

  @Test fun startFromState() {
    val workflow = reactor.startRootWorkflow(SecondState("hello"))

    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()
  }

  @Test fun stateCompletesWhenAbandoned() {
    val workflow = reactor.startRootWorkflow(SecondState("hello"))

    workflow.state.subscribe(stateSub)
    workflow.cancel()

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertTerminated()
  }

  @Test fun stateStaysCompletedForLateSubscribersWhenAbandoned() {
    val workflow = reactor.startRootWorkflow(SecondState("hello"))
    workflow.cancel()

    workflow.state.subscribe(stateSub)
    stateSub.assertNoValues()
    stateSub.assertTerminated()
  }

  @Test fun resultCompletesWhenAbandoned() {
    val workflow = reactor.startRootWorkflow(SecondState("hello"))
    val resultSub = workflow.result.test()
    workflow.cancel()
    resultSub.assertNoValues()
    resultSub.assertTerminated()
  }

  @Test fun singleStateChangeAndThenFinish() {
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> {
        return when (state) {
          is FirstState -> EnterState(SecondState("${state.value} ${state.value}"))
          is SecondState -> FinishWith("all done")
        }.run { Single.just(this) }
      }
    }

    start("hello")
    // We never see the values b/c the machine races through its states and finishes
    // the instant we start it.
    stateSub.assertNoValues()
    stateSub.assertComplete()
    resultSub.assertValue("all done")
  }

  @Test fun delayedStateChange() {
    val secondStateSubject = PublishSubject.create<TestState>()

    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> {
        return when (state) {
          is FirstState -> secondStateSubject.firstOrError().map { EnterState(it) }
          is SecondState -> FinishWith("all done").run { Single.just(this) }
        }
      }
    }

    start("hello")

    stateSub.assertValue(FirstState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()

    secondStateSubject.onNext(SecondState("foo"))
    stateSub.assertValues(FirstState("hello"), SecondState("foo"))
    stateSub.assertComplete()
    resultSub.assertValue("all done")
  }

  @Test fun whenReactThrowsDirectly() {
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> {
        throw RuntimeException("((angery))")
      }
    }

    start("foo")

    assertThat(stateSub.errors().single())
        .isExactlyInstanceOf(ReactorException::class.java)
        .hasMessageContaining("threw RuntimeException: ((angery))")
    assertThat(resultSub.errors().single())
        .isExactlyInstanceOf(ReactorException::class.java)
        .hasMessageContaining("threw RuntimeException: ((angery))")
  }

  @Test fun whenReactSingleThrows() {
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> {
        return Single.error(RuntimeException("((angery))"))
      }
    }

    start("foo")

    assertThat(stateSub.errors().single())
        .isExactlyInstanceOf(ReactorException::class.java)
        .hasMessageContaining("threw RuntimeException: ((angery))")
    assertThat(resultSub.errors().single())
        .isExactlyInstanceOf(ReactorException::class.java)
        .hasMessageContaining("threw RuntimeException: ((angery))")
  }

  @Test fun singleIsUnsubscribed_onAbandonment() {
    var subscribeCount = 0
    var unsubscribeCount = 0
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> {
        return never<Reaction<TestState, String>>()
            .doOnSubscribe { subscribeCount++ }
            .doOnDispose { unsubscribeCount++ }
      }
    }

    start("foo")

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(unsubscribeCount).isEqualTo(0)

    workflow.cancel()

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(unsubscribeCount).isEqualTo(1)
  }

  @Test fun exceptionIsPropagated_whenStateSubscriberThrowsFromSecondOnNext_asynchronously() {
    val trigger = SingleSubject.create<Unit>()
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: EventChannel<Nothing>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> =
        when (state) {
          is FirstState -> trigger.map { EnterState(SecondState("")) }
          is SecondState -> super.onReact(state, events, workflows)
        }
    }
    start("foo")
    stateSub.dispose()

    workflow.state.subscribe { println("noop") /* No-op */ }
    workflow.state
        .ofType(SecondState::class.java)
        .subscribe { throw RuntimeException("fail") }

    try {
      rethrowingUncaughtExceptions {
        trigger.onSuccess(Unit)
      }
      fail("Expected exception.")
    } catch (e: OnErrorNotImplementedException) {
      assertThat(e.cause)
          .isInstanceOf(RuntimeException::class.java)
          .hasMessage("fail")
    }
  }

  @Test fun acceptsEventsBeforeSubscriptions() {
    val reactor = object : Reactor<FirstState, String, String> {
      override fun onReact(
        state: FirstState,
        events: EventChannel<String>,
        workflows: WorkflowPool
      ): Single<out Reaction<FirstState, String>> {
        return events.select {
          onEvent<String> {
            EnterState(FirstState(it))
          }
        }
      }
    }

    // Unused, but just to be certain it doesn't get gc'd on us. Silly, I know.
    val workflow = reactor.startRootWorkflow(FirstState("Hello"))
    workflow.sendEvent("Fnord")
    // No crash, no bug!
  }

  @Test fun buffersEvents_whenSelectNotCalled() {
    val proceedToSecondState = CompletableSubject.create()
    val reactor = object : Reactor<TestState, String, String> {
      override fun onReact(
        state: TestState,
        events: EventChannel<String>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> = when (state) {
        is FirstState -> proceedToSecondState.andThen(just(EnterState(SecondState(""))))
        is SecondState -> events.select {
          onEvent<String> { FinishWith(it) }
        }
      }
    }
    val workflow = reactor.startRootWorkflow(FirstState("Hello"))
    workflow.result.subscribe(resultSub)

    // This event should get buffered…
    workflow.sendEvent("Fnord")

    // …but not accepted yet.
    resultSub.assertNotTerminated()

    // This triggers the events.select, which should finish the workflow.
    proceedToSecondState.onComplete()
    resultSub.assertValue("Fnord")
  }

  @Test fun buffersEvents_whenReentrant() {
    lateinit var workflow: Workflow<TestState, String, String>
    val reactor = object : Reactor<TestState, String, String> {
      override fun onReact(
        state: TestState,
        events: EventChannel<String>,
        workflows: WorkflowPool
      ): Single<out Reaction<TestState, String>> = when (state) {
        is FirstState -> events.select {
          onEvent<String> { event ->
            workflow.sendEvent("i heard you like events")
            EnterState(SecondState(event))
          }
        }
        is SecondState -> events.select {
          onEvent<String> { FinishWith(it) }
        }
      }
    }
    workflow = reactor.startRootWorkflow(FirstState(""))
    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(FirstState(""))
    resultSub.assertNotTerminated()

    workflow.sendEvent("foo")
    stateSub.assertValues(FirstState(""), SecondState("foo"))
    resultSub.assertValue("i heard you like events")
  }

  @Test fun rejectsEvents_whenSelectCalled_withNoEventCases() {
    val reactor = object : Reactor<FirstState, String, String> {
      override fun onReact(
        state: FirstState,
        events: EventChannel<String>,
        workflows: WorkflowPool
      ): Single<out Reaction<FirstState, String>> = events.select {
        // No cases.
      }
    }
    val workflow = reactor.startRootWorkflow(FirstState("Hello"))
    workflow.state.subscribe(stateSub)

    workflow.sendEvent("Fnord")

    stateSub.assertError { "Expected EventChannel to accept event: Fnord" in it.message!! }
  }
}

private sealed class TestState {
  data class FirstState(val value: String) : TestState()
  data class SecondState(val value: String) : TestState()
}
