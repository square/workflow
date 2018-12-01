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
package com.squareup.workflow.rx2

import com.squareup.workflow.rx2.CoroutineEventChannelTest.Events.Click
import com.squareup.workflow.rx2.CoroutineEventChannelTest.Events.Dismiss
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.MaybeSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class CoroutineEventChannelTest {

  sealed class Events {
    object Click : Events() {
      override fun toString() = javaClass.simpleName!!
    }

    object Dismiss : Events() {
      override fun toString() = javaClass.simpleName!!
    }
  }

  enum class State {
    InitialState,
    OtherState
  }

  @JvmField @Rule val thrown = ExpectedException.none()!!

  private val resultSub = TestObserver<String>()

  private var events = Channel<Events>(UNLIMITED)

  @Test fun selectEvent_propagatesResult() {
    events.asEventChannel()
        .select<String> {
          onEvent<Click> { it.toString() }
        }
        .subscribe(resultSub)

    events.offer(Click)

    resultSub.assertValue("Click")
  }

  @Test fun selectEvent_propagatesExceptionsFromBuilder() {
    events.asEventChannel()
        .select<String> {
          throw RuntimeException("fail")
        }
        .subscribe(resultSub)

    resultSub.assertError { it is RuntimeException && it.message == "fail" }
  }

  @Test fun selectEvent_propagatesExceptionsFromHandlers() {
    events.asEventChannel()
        .select<String> {
          onEvent<Click> { throw RuntimeException("fail") }
        }
        .subscribe(resultSub)

    events.offer(Click)

    assertThat(resultSub.errors().single())
        .isExactlyInstanceOf(RuntimeException::class.java)
        .hasMessage("fail")
  }

  @Test fun selectEvent_throwsWhenEventNotAccepted() {
    events.asEventChannel()
        .select<String> {
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    events.offer(Dismiss)

    resultSub.assertError {
      it is IllegalStateException && it.message == "Expected EventChannel to accept event: Dismiss"
    }
  }

  @Test fun selectEvent_doesNotThrow_whenReentered() {
    val receivedEvents = mutableListOf<Events>()

    // Use map and doOnSuccess to simulate how code will actually use this – Reactors don't
    // subscribe directly.
    events.asEventChannel()
        .select<Unit> {
          onEvent<Click> { firstEvent ->
            receivedEvents += firstEvent

            // In real code, the handler for the event may synchronously transition to another state,
            // which in turn calls selectEvent again. However, since the first handler won't have
            // returned yet, it still has observers, although they're effectively obsolete. Calling
            // selectEvent in this state should *not* complain about multiple calls.
            events.asEventChannel()
                .select<Unit> {
                  onEvent<Dismiss> {
                    receivedEvents += it
                  }
                }
                .subscribe()
          }
        }
        .subscribe()

    events.offer(Click)
    events.offer(Dismiss)

    assertThat(receivedEvents).containsExactly(Click, Dismiss)
  }

  @Test fun selectEvent_clearsState_onUnsubscribe() {
    events.asEventChannel()
        .select<String> {}
        .subscribe(resultSub)

    // This should tell the selectEvent "We don't care anymore" and allow another call.
    resultSub.dispose()

    events.asEventChannel()
        .select<Unit> {}
  }

  @Test fun selectEvent_isNotRequired() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "done" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
  }

  @Test fun onSuccess_singleCase() {
    val relay = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> { onSuccess(relay) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("got foo")
  }

  @Test fun onSuccess_multipleCases() {
    val relay1 = SingleSubject.create<String>()
    val relay2 = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onSuccess(relay1) { "1 got $it" }
          onSuccess(relay2) { "2 got $it" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay2.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("2 got foo")
  }

  @Test fun onSuccess_whenTerminatesWithError() {
    val subject = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> { onSuccess(subject) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    val e = RuntimeException("oops")
    subject.onError(e)

    resultSub.assertNoValues()
    resultSub.assertError(e)
  }

  @Test fun onSuccess_withOnEvent_whenSingleWins() {
    val relay = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onSuccess(relay) { "relay got $it" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("relay got foo")
  }

  @Test fun onSuccess_withOnEvent_whenEventWins() {
    val relay = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onSuccess(relay) { "relay got $it" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    events.offer(Click)

    resultSub.assertComplete()
    resultSub.assertValue("clicked")
  }

  @Test fun onSuccess_unsubscribes_whenLosesRace_noExceptions() {
    var subscribeCalls = 0
    var disposeCalls = 0
    val observable = SingleSubject.create<String>()
        .doOnSubscribe { subscribeCalls++ }
        .doOnDispose { disposeCalls++ }
    events.asEventChannel()
        .select<String> {
          onSuccess(observable) { "shouldn't happen" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    assertThat(subscribeCalls).isEqualTo(1)
    assertThat(disposeCalls).isEqualTo(0)

    events.offer(Click)

    resultSub.assertComplete()
    assertThat(disposeCalls).isEqualTo(1)
  }

  @Test fun onSuccess_unsubscribes_whenSelectThrows() {
    var subscribeCalls = 0
    var disposeCalls = 0
    val observable = SingleSubject.create<String>()
        .doOnSubscribe { subscribeCalls++ }
        .doOnDispose { disposeCalls++ }
    events.asEventChannel()
        .select<String> {
          onSuccess(observable) { "shouldn't happen" }
          throw RuntimeException("fail")
        }
        .subscribe(resultSub)

    resultSub.assertErrorMessage("fail")
    assertThat(disposeCalls).isEqualTo(1)
  }

  @Test fun onMaybe_singleCase() {
    val relay = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> { onMaybeSuccessOrNever(relay) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("got foo")
  }

  @Test fun onMaybe_multipleCases() {
    val relay1 = MaybeSubject.create<String>()
    val relay2 = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(relay1) { "1 got $it" }
          onMaybeSuccessOrNever(relay2) { "2 got $it" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay2.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("2 got foo")
  }

  @Test fun onMaybe_doesntEmit_whenCompletesWithoutValue() {
    val subject = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> { onMaybeSuccessOrNever(subject) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    subject.onComplete()

    resultSub.assertNotTerminated()
  }

  @Test fun onMaybe_allowsOtherCasesToWin_whenCompletesWithoutValue() {
    val subject = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(subject) { "got $it" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    subject.onComplete()
    events.offer(Click)

    resultSub.assertValue("clicked")
  }

  @Test fun onMaybe_whenTerminatesWithError() {
    val subject = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> { onMaybeSuccessOrNever(subject) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    val e = RuntimeException("oops")
    subject.onError(e)

    resultSub.assertNoValues()
    resultSub.assertError(e)
  }

  @Test fun onMaybe_withOnEvent_whenMaybeWins() {
    val relay = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(relay) { "relay got $it" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    relay.onSuccess("foo")

    resultSub.assertComplete()
    resultSub.assertValue("relay got foo")
  }

  @Test fun onMaybe_withOnEvent_whenEventWins() {
    val relay = MaybeSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(relay) { "relay got $it" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    events.offer(Click)

    resultSub.assertComplete()
    resultSub.assertValue("clicked")
  }

  @Test fun onMaybe_unsubscribes_whenLosesRace_noExceptions() {
    var subscribeCalls = 0
    var disposeCalls = 0
    val observable = MaybeSubject.create<String>()
        .doOnSubscribe { subscribeCalls++ }
        .doOnDispose { disposeCalls++ }
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(observable) { "shouldn't happen" }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    assertThat(subscribeCalls).isEqualTo(1)
    assertThat(disposeCalls).isEqualTo(0)

    events.offer(Click)

    resultSub.assertComplete()
    assertThat(disposeCalls).isEqualTo(1)
  }

  @Test fun onMaybe_unsubscribes_whenSelectThrows() {
    var subscribeCalls = 0
    var disposeCalls = 0
    val observable = MaybeSubject.create<String>()
        .doOnSubscribe { subscribeCalls++ }
        .doOnDispose { disposeCalls++ }
    events.asEventChannel()
        .select<String> {
          onMaybeSuccessOrNever(observable) { "shouldn't happen" }
          throw RuntimeException("fail")
        }
        .subscribe(resultSub)

    resultSub.assertErrorMessage("fail")
    assertThat(disposeCalls).isEqualTo(1)
  }

  @Test fun exceptionFromSingleHandler_isPropagated() {
    val relay = PublishSubject.create<String>()
    events.asEventChannel()
        .select<String> {
          onSuccess(relay.firstOrError()) {
            throw RuntimeException("fail")
          }
        }
        .subscribe(resultSub)

    relay.onNext("boo")

    resultSub.assertError { it is RuntimeException && it.message == "fail" }
  }

  @Test fun observableEmission_isIgnored_fromEventHandler() {
    val relay = PublishSubject.create<String>()
    events.asEventChannel()
        .select<String> { onEvent<Click> { relay.onNext("boo"); "clicked" } }
        .subscribe(resultSub)

    events.offer(Click)

    resultSub.assertComplete()
    resultSub.assertValue("clicked")
  }

  @Test fun closeWhileSelect_neverSignals() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked" } }
        .subscribe(resultSub)

    events.close()

    resultSub.assertNoValues()
    resultSub.assertNotTerminated()
  }

  @Test fun exceptionFromSubscriber_reportsToUncaughtHandler_whenEventNotQueued() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked" } }
        .subscribe { event ->
          throw RuntimeException("fail: $event")
        }

    try {
      rethrowingUncaughtExceptions {
        events.offer(Click)
      }
      fail("Expected exception.")
    } catch (e: UndeliverableException) {
      assertThat(e.cause).isExactlyInstanceOf(RuntimeException::class.java)
          .hasMessage("fail: clicked")
    }
  }

  @Suppress("UNCHECKED_CAST")
  @Test fun unsubscribe_cancelsSelect() {
    val firstSub = events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked 1" } }
        .test()
    firstSub.dispose()

    val secondSub = events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked 2" } }
        .test() as TestObserver<String>

    assertTrue(events.offer(Click))

    firstSub.assertNoValues()
    secondSub.assertValue("clicked 2")
  }
}
