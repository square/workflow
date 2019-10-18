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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.internal.util.rethrowingUncaughtExceptions
import com.squareup.workflow.legacy.EnterState
import com.squareup.workflow.legacy.FinishWith
import com.squareup.workflow.legacy.Reaction
import com.squareup.workflow.legacy.ReactorException
import com.squareup.workflow.legacy.Workflow
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.rx2.TestState.FirstState
import com.squareup.workflow.legacy.rx2.TestState.SecondState
import io.reactivex.Single
import io.reactivex.Single.just
import io.reactivex.Single.never
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import kotlin.test.fail

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
    workflow = reactor.doLaunch(FirstState(input), WorkflowPool())
        .apply {
          state.subscribe(stateSub)
          result.subscribe(resultSub)
        }
  }

  @Test fun `start new initial state after start new`() {
    start("hello")
    stateSub.assertValue(FirstState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()
  }

  @Test fun `start from state`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())

    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertNotTerminated()
    resultSub.assertNotTerminated()
  }

  @Test fun `state completes when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())

    workflow.state.subscribe(stateSub)
    workflow.cancel()

    stateSub.assertValue(SecondState("hello"))
    stateSub.assertTerminated()
  }

  @Test fun `state stays completed for late subscribers when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())
    workflow.cancel()

    workflow.state.subscribe(stateSub)
    stateSub.assertNoValues()
    stateSub.assertTerminated()
  }

  @Test fun `result completes when abandoned`() {
    val workflow = reactor.doLaunch(SecondState("hello"), WorkflowPool())
    val resultSub = workflow.result.test()
    workflow.cancel()
    resultSub.assertNoValues()
    resultSub.assertTerminated()
  }

  @Test fun `single state change and then finish`() {
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

  @Test fun `delayed state change`() {
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

  @Test fun `when react throws directly`() {
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

    stateSub.errors()
        .single()
        .let {
          assertThat(it).isInstanceOf(ReactorException::class.java)
          assertThat(it).hasMessageThat()
              .contains("threw RuntimeException: ((angery))")
        }
    resultSub.errors()
        .single()
        .let {
          assertThat(it).isInstanceOf(ReactorException::class.java)
          assertThat(it).hasMessageThat()
              .contains("threw RuntimeException: ((angery))")
        }
  }

  @Test fun `when react Single throws`() {
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

    stateSub.errors()
        .single()
        .let {
          assertThat(it).isInstanceOf(ReactorException::class.java)
          assertThat(it).hasMessageThat()
              .contains("threw RuntimeException: ((angery))")
        }
    resultSub.errors()
        .single()
        .let {
          assertThat(it).isInstanceOf(ReactorException::class.java)
          assertThat(it).hasMessageThat()
              .contains("threw RuntimeException: ((angery))")
        }
  }

  @Test fun `single is unsubscribed on abandonment`() {
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

  @Test
  fun `exception is propagated when state subscriber throws from second onNext_asynchronously`() {
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
      assertThat(e).hasCauseThat()
          .isInstanceOf(RuntimeException::class.java)
      assertThat(e).hasCauseThat()
          .hasMessageThat()
          .isEqualTo("fail")
    }
  }

  @Test fun `accepts events before subscriptions`() {
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
    val workflow = reactor.doLaunch(FirstState("Hello"), WorkflowPool())
    workflow.sendEvent("Fnord")
    // No crash, no bug!
  }

  @Test fun `buffers events when select not called`() {
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
    val workflow = reactor.doLaunch(FirstState("Hello"), WorkflowPool())
    workflow.result.subscribe(resultSub)

    // This event should get buffered…
    workflow.sendEvent("Fnord")

    // …but not accepted yet.
    resultSub.assertNotTerminated()

    // This triggers the events.select, which should finish the workflow.
    proceedToSecondState.onComplete()
    resultSub.assertValue("Fnord")
  }

  @Test fun `buffers events when reentrant`() {
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
    workflow = reactor.doLaunch(FirstState(""), WorkflowPool())
    workflow.state.subscribe(stateSub)
    workflow.result.subscribe(resultSub)

    stateSub.assertValue(FirstState(""))
    resultSub.assertNotTerminated()

    workflow.sendEvent("foo")
    stateSub.assertValues(FirstState(""), SecondState("foo"))
    resultSub.assertValue("i heard you like events")
  }

  @Test fun `rejects events when select called with no event cases`() {
    val reactor = object : Reactor<FirstState, String, String> {
      override fun onReact(
        state: FirstState,
        events: EventChannel<String>,
        workflows: WorkflowPool
      ): Single<out Reaction<FirstState, String>> = events.select {
        // No cases.
      }
    }
    val workflow = reactor.doLaunch(FirstState("Hello"), WorkflowPool())
    workflow.state.subscribe(stateSub)

    workflow.sendEvent("Fnord")

    stateSub.assertError { "Expected EventChannel to accept event: Fnord" in it.message!! }
  }
}

private sealed class TestState {
  data class FirstState(val value: String) : TestState()
  data class SecondState(val value: String) : TestState()
}
