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
// This test tests code that is marked as deprecated, so we can ignore those warnings.
@file:Suppress("DEPRECATION", "EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.legacy.rx2

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.internal.util.rethrowingUncaughtExceptions
import com.squareup.workflow.legacy.Finished
import com.squareup.workflow.legacy.Running
import com.squareup.workflow.legacy.Workflow
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.WorkflowPool.Handle
import com.squareup.workflow.legacy.WorkflowPool.Launcher
import com.squareup.workflow.legacy.register
import com.squareup.workflow.legacy.rx2.CoroutineEventChannelTest.Events.Click
import com.squareup.workflow.legacy.rx2.CoroutineEventChannelTest.Events.Dismiss
import com.squareup.workflow.legacy.workflow
import io.reactivex.Single
import io.reactivex.Single.just
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertFailsWith
import kotlin.test.fail

class CoroutineEventChannelTest {

  sealed class Events {
    object Click : Events() {
      override fun toString() = javaClass.simpleName!!
    }

    object Dismiss : Events() {
      override fun toString() = javaClass.simpleName!!
    }
  }

  private val pool = WorkflowPool()
  private val resultSub = TestObserver<String>()
  private var events = Channel<Events>(UNLIMITED)

  @Test fun `select event propagates result`() {
    events.asEventChannel()
        .select<String> {
          onEvent<Click> { it.toString() }
        }
        .subscribe(resultSub)

    events.offer(Click)

    resultSub.assertValue("Click")
  }

  @Test fun `select event propagates exceptions from builder`() {
    events.asEventChannel()
        .select<String> {
          throw RuntimeException("fail")
        }
        .subscribe(resultSub)

    resultSub.assertError { it is RuntimeException && it.message == "fail" }
  }

  @Test fun `select event propagates exceptions from handlers`() {
    events.asEventChannel()
        .select<String> {
          onEvent<Click> { throw RuntimeException("fail") }
        }
        .subscribe(resultSub)

    events.offer(Click)

    resultSub.errors()
        .single()
        .let {
          assertThat(it).isInstanceOf(RuntimeException::class.java)
          assertThat(it).hasMessageThat()
              .isEqualTo("fail")
        }
  }

  @Test fun `select event throws when event not accepted`() {
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

  @Test fun `select event does not throw when reentered`() {
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

  @Test fun `selectEvent clears state on unsubscribe`() {
    events.asEventChannel()
        .select<String> {}
        .subscribe(resultSub)

    // This should tell the selectEvent "We don't care anymore" and allow another call.
    resultSub.dispose()

    events.asEventChannel()
        .select<Unit> {}
  }

  @Test fun `selectEvent is not required`() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "done" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
  }

  @Test fun `onSuspending single case`() {
    val deferred = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> { onSuspending({ "got $it" }) { deferred.await() } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    deferred.complete("foo")

    resultSub.assertComplete()
    resultSub.assertValue("got foo")
  }

  @Test fun `onSuspending multiple cases`() {
    val deferred1 = CompletableDeferred<String>()
    val deferred2 = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> {
          onSuspending({ "1 got $it" }) { deferred1.await() }
          onSuspending({ "2 got $it" }) { deferred2.await() }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    deferred2.complete("foo")

    resultSub.assertComplete()
    resultSub.assertValue("2 got foo")
  }

  @Test fun `onSuspending when terminates with error`() {
    val deferred = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> { onSuspending({ "got $it" }) { deferred.await() } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    val e = RuntimeException("oops")
    deferred.completeExceptionally(e)

    resultSub.assertNoValues()
    // Can't use assertError since the exception is cloned by the coroutine runtime for stacktrace
    // recovery.
    resultSub.assertError { it is RuntimeException && it.message == "oops" }
  }

  @Test fun `onSuspending with onEvent when Single wins`() {
    val deferred = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> {
          onSuspending({ "got $it" }) { deferred.await() }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    deferred.complete("foo")

    resultSub.assertComplete()
    resultSub.assertValue("got foo")
  }

  @Test fun `onSuspending with onEvent when Event wins`() {
    val deferred = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> {
          onSuspending({ "got $it" }) { deferred.await() }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    events.offer(Click)

    resultSub.assertComplete()
    resultSub.assertValue("clicked")
  }

  @Test fun `onSuspending cancels coroutine when loses race without errors`() {
    var invocations = 0
    var cancellations = 0
    events.asEventChannel()
        .select<String> {
          onSuspending({ error("shouldn't happen") }) {
            invocations++
            suspendCancellableCoroutine<Nothing> {
              it.invokeOnCancellation { cancellations++ }
            }
          }
          onEvent<Click> { "clicked" }
        }
        .subscribe(resultSub)

    assertThat(invocations).isEqualTo(1)
    assertThat(cancellations).isEqualTo(0)

    events.offer(Click)

    resultSub.assertComplete()
    assertThat(invocations).isEqualTo(1)
    assertThat(cancellations).isEqualTo(1)
  }

  @Test fun `onSuspending cancels coroutine when select throws`() {
    var cancellations = 0
    events.asEventChannel()
        .select<String> {
          onSuspending({ error("shouldn't happen") }) {
            suspendCancellableCoroutine<Nothing> {
              it.invokeOnCancellation { cancellations++ }
            }
          }
          throw ExpectedException()
        }
        .subscribe(resultSub)

    resultSub.assertError(ExpectedException::class.java)
    assertThat(cancellations).isEqualTo(1)
  }

  @Test fun `onSuspending handler exception is propagated`() {
    val deferred = CompletableDeferred<String>()
    events.asEventChannel()
        .select<String> {
          onSuspending({ throw ExpectedException() }) { deferred.await() }
        }
        .subscribe(resultSub)

    resultSub.assertNoErrors()

    deferred.complete("boo")

    resultSub.assertError(ExpectedException::class.java)
  }

  @Test fun `onWorkerResult succeeds`() {
    val trigger = CompletableSubject.create()
    @Suppress("RedundantLambdaArrow")
    val worker = singleWorker { it: String ->
      trigger.andThen(just("worker: $it"))
    }
    events.asEventChannel()
        .select<String> { pool.onWorkerResult(worker, "foo") { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    trigger.onComplete()

    resultSub.assertValue("got worker: foo")
  }

  @Test fun `onWorkerResult throws from worker`() {
    val trigger = CompletableSubject.create()
    val worker = singleWorker<String, String> {
      trigger.andThen(Single.error<String>(ExpectedException()))
    }
    events.asEventChannel()
        .select<String> { pool.onWorkerResult(worker, "oops") { fail() } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    trigger.onComplete()

    resultSub.assertError(ExpectedException::class.java)
  }

  @Test fun `onNextDelegateReaction reports state`() {
    val launcher = object : Launcher<String, String, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ): Workflow<String, String, String> = GlobalScope.workflow(Unconfined) { s, e ->
        s.send("$initialState -> ${e.receive()}")
        suspendCoroutine<Nothing> { }
      }
    }
    val handle = WorkflowPool.handle(launcher::class, "foo")
    val state = DelegateWorkflowState(handle)
    pool.register(launcher)
    events.asEventChannel()
        .select<String> {
          pool.onWorkflowUpdate(state.workflow) {
            when (it) {
              is Running -> "enter ${it.handle.state}"
              is Finished -> "finish ${it.result}"
            }
          }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    pool.input(handle)
        .sendEvent("bar")

    resultSub.assertValue("enter foo -> bar")
  }

  @Test fun `onNextDelegateReaction reports result`() {
    val launcher = object : Launcher<String, String, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ): Workflow<String, String, String> = GlobalScope.workflow(Unconfined) { _, e ->
        "$initialState -> ${e.receive()}"
      }
    }
    val handle = WorkflowPool.handle(launcher::class, "foo")
    val state = DelegateWorkflowState(handle)
    pool.register(launcher)
    events.asEventChannel()
        .select<String> {
          pool.onWorkflowUpdate(state.workflow) {
            when (it) {
              is Running -> "enter ${it.handle}"
              is Finished -> "finish ${it.result}"
            }
          }
        }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    pool.input(handle)
        .sendEvent("bar")

    resultSub.assertValue("finish foo -> bar")
  }

  @Test fun `onSuccess single case`() {
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

  @Test fun `onSuccess multiple cases`() {
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

  @Test fun `onSuccess when terminates with error`() {
    val subject = SingleSubject.create<String>()
    events.asEventChannel()
        .select<String> { onSuccess(subject) { "got $it" } }
        .subscribe(resultSub)

    resultSub.assertNotTerminated()
    resultSub.assertNoValues()

    val e = ExpectedException()
    subject.onError(e)

    resultSub.assertNoValues()
    // The coroutines runtime, at least in debug mode, will actually clone the exception to augment
    // the stack trace, so we can't rely on object identity.
    resultSub.assertError(ExpectedException::class.java)
  }

  @Test fun `onSuccess with onEvent when Single wins`() {
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

  @Test fun `onSuccess with onEvent when Event wins`() {
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

  @Test fun `onSuccess unsubscribes when loses race no exceptions`() {
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

  @Test fun `onSuccess unsubscribes when select throws`() {
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
    assertThat(subscribeCalls).isEqualTo(1)
    assertThat(disposeCalls).isEqualTo(1)
  }

  @Test fun `onSuccess handler exception is propagated`() {
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

  @Test fun `observable emission is ignored from event handler`() {
    val relay = PublishSubject.create<String>()
    events.asEventChannel()
        .select<String> { onEvent<Click> { relay.onNext("boo"); "clicked" } }
        .subscribe(resultSub)

    events.offer(Click)

    resultSub.assertComplete()
    resultSub.assertValue("clicked")
  }

  @Test fun `close while select never signals`() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked" } }
        .subscribe(resultSub)

    events.close()

    resultSub.assertNoValues()
    resultSub.assertNotTerminated()
  }

  @Test fun `exception from subscriber reports to uncaught handler when event not queued`() {
    events.asEventChannel()
        .select<String> { onEvent<Click> { "clicked" } }
        .subscribe { event ->
          throw RuntimeException("fail: $event")
        }

    assertFailsWith<UndeliverableException> {
      rethrowingUncaughtExceptions {
        events.offer(Click)
      }
    }.let {
      assertThat(it).hasCauseThat()
          .isInstanceOf(RuntimeException::class.java)
      assertThat(it).hasCauseThat()
          .hasMessageThat()
          .isEqualTo("fail: clicked")
    }
  }

  @Suppress("UNCHECKED_CAST")
  @Test fun `unsubscribe cancels select`() {
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

private class ExpectedException : RuntimeException()

private data class DelegateWorkflowState(
  val workflow: Handle<String, String, String>
)
