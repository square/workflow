package com.squareup.reactor.rx2

import com.squareup.reactor.EnterState
import com.squareup.reactor.FinishWith
import com.squareup.reactor.Reaction
import com.squareup.reactor.ReactorException
import com.squareup.reactor.rx2.TestState.FirstState
import com.squareup.reactor.rx2.TestState.SecondState
import com.squareup.workflow.Rx2Workflow
import io.reactivex.Single
import io.reactivex.Single.just
import io.reactivex.Single.never
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ReactorAsWorkflowIntegrationTest {

  private open class MockReactor : Rx2Reactor<TestState, Nothing, String> {
    override fun onReact(
      state: TestState,
      events: Rx2EventChannel<Nothing>
    ): Single<out Reaction<TestState, String>> = never()
  }

  @JvmField @Rule val thrown = ExpectedException.none()!!

  private val stateSub = TestObserver<TestState>()
  private val resultSub = TestObserver<String>()

  private var reactor: Rx2Reactor<TestState, Nothing, String> = MockReactor()
  private lateinit var workflow: Rx2Workflow<TestState, Nothing, String>

  @Suppress("UNCHECKED_CAST")
  fun start(input: String) {
    workflow = reactor.startWorkflow(FirstState(input))
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
    val workflow = reactor.startWorkflow(SecondState("hello"))

    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()
  }

  @Test fun stateCompletesWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))

    workflow.state.subscribe(stateSub)
    workflow.abandon()

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertTerminated()
  }

  @Test fun stateStaysCompletedForLateSubscribersWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))
    workflow.abandon()

    workflow.state.subscribe(stateSub)
    stateSub.assertNoValues()
    stateSub.assertTerminated()
  }

  @Test fun resultDoesNotCompleteWhenAbandoned() {
    val workflow = reactor.startWorkflow(SecondState("hello"))
    val resultSub = workflow.result.test()
    workflow.abandon()
    resultSub.assertNotTerminated()
  }

  @Test fun singleStateChangeAndThenFinish() {
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: Rx2EventChannel<Nothing>
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
        events: Rx2EventChannel<Nothing>
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
        events: Rx2EventChannel<Nothing>
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
        events: Rx2EventChannel<Nothing>
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
        events: Rx2EventChannel<Nothing>
      ): Single<out Reaction<TestState, String>> {
        return never<Reaction<TestState, String>>()
            .doOnSubscribe { subscribeCount++ }
            .doOnDispose { unsubscribeCount++ }
      }
    }

    start("foo")

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(unsubscribeCount).isEqualTo(0)

    workflow.abandon()

    assertThat(subscribeCount).isEqualTo(1)
    assertThat(unsubscribeCount).isEqualTo(1)
  }

  @Test fun reactorIsNotAbandoned_whenInFinishedState() {
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: Rx2EventChannel<Nothing>
      ): Single<out Reaction<TestState, String>> {
        return just(FinishWith("all done"))
      }

      override fun onAbandoned(state: TestState) {
        fail("Expected onAbandoned not to be called.")
      }
    }

    start("starting")
    workflow.abandon()
  }

  @Test fun reactorIsAbandoned_onAbandonment() {
    val log = StringBuilder()

    reactor = object : MockReactor() {
      override fun onAbandoned(state: TestState) {
        log.append("+reactor.onAbandoned")
      }
    }

    start("starting")
    workflow.state.ignoreElements()
        .subscribe { log.append("+workflow.Completed") }
    workflow.abandon()

    // Docs promise that the workflow doesn't complete until the reactor has a chance
    // to do its abandon thing.
    assertThat(log.toString())
        .isEqualToIgnoringWhitespace("+reactor.onAbandoned+workflow.Completed")
  }

  @Test fun exceptionIsPropagated_whenStateSubscriberThrowsFromSecondOnNext_asynchronously() {
    val trigger = PublishSubject.create<Unit>()
    reactor = object : MockReactor() {
      override fun onReact(
        state: TestState,
        events: Rx2EventChannel<Nothing>
      ): Single<out Reaction<TestState, String>> =
        when (state) {
          is FirstState -> trigger.firstOrError().map { EnterState(SecondState("")) }
          is SecondState -> super.onReact(state, events)
        }
    }
    start("foo")
    stateSub.dispose()

    workflow.state.subscribe { println("noop") /* No-op */ }
    workflow.state
        .ofType(SecondState::class.java)
        .subscribe { throw RuntimeException("fail") }

    thrown.expect(RuntimeException::class.java)
    thrown.expectMessage("fail")

    rethrowingUncaughtExceptions {
      trigger.onNext(Unit)
    }
  }

  // RF-1426
  @Test fun acceptsEventsBeforeSubscriptions() {
    val reactor = object : Rx2Reactor<FirstState, String, String> {
      override fun onReact(
        state: FirstState,
        events: Rx2EventChannel<String>
      ): Single<out Reaction<FirstState, String>> {
        return events.select {
          onEvent<String> {
            EnterState(FirstState(it))
          }
        }
      }
    }

    // Unused, but just to be certain it doesn't get gc'd on us. Silly, I know.
    val workflow = reactor.startWorkflow(FirstState("Hello"))
    workflow.sendEvent("Fnord")
    // No crash, no bug!
  }

  @Test fun buffersEvents_whenSelectNotCalled() {
    val proceedToSecondState = CompletableSubject.create()
    val reactor = object : Rx2Reactor<TestState, String, String> {
      override fun onReact(
        state: TestState,
        events: Rx2EventChannel<String>
      ): Single<out Reaction<TestState, String>> = when (state) {
        is FirstState -> proceedToSecondState.andThen(just(EnterState(SecondState(""))))
        is SecondState -> events.select {
          onEvent<String> { FinishWith(it) }
        }
      }
    }
    val workflow = reactor.startWorkflow(FirstState("Hello"))
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
    lateinit var workflow: Rx2Workflow<TestState, String, String>
    val reactor = object : Rx2Reactor<TestState, String, String> {
      override fun onReact(
        state: TestState,
        events: Rx2EventChannel<String>
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
    workflow = reactor.startWorkflow(FirstState(""))
    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(FirstState(""))
    resultSub.assertNotTerminated()

    workflow.sendEvent("foo")
    stateSub.assertValues(FirstState(""), SecondState("foo"))
    resultSub.assertValue("i heard you like events")
  }

  @Test fun rejectsEvents_whenSelectCalled_withNoEventCases() {
    val reactor = object : Rx2Reactor<FirstState, String, String> {
      override fun onReact(
        state: FirstState,
        events: Rx2EventChannel<String>
      ): Single<out Reaction<FirstState, String>> = events.select {
        // No cases.
      }
    }
    val workflow = reactor.startWorkflow(FirstState("Hello"))
    workflow.state.subscribe(stateSub)

    workflow.sendEvent("Fnord")

    stateSub.assertError { "Expected EventChannel to accept event: Fnord" in it.message!! }
  }
}

private sealed class TestState {
  data class FirstState(val value: String) : TestState()
  data class SecondState(val value: String) : TestState()
}
