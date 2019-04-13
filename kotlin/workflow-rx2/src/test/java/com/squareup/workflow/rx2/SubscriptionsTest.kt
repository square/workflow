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
package com.squareup.workflow.rx2

import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.testing.testFromStart
import com.squareup.workflow.util.ChannelUpdate
import com.squareup.workflow.util.ChannelUpdate.Closed
import com.squareup.workflow.util.ChannelUpdate.Value
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class SubscriptionsTest {

  /**
   * Workflow that has a single Boolean state: whether to subscribe to [subject] or not.
   *
   * The initial value for the state is taken from the input. After that, the input is ignored.
   *
   * [compose] returns a setter that will change the subscription state.
   */
  private class SubscriberWorkflow(
    subject: Observable<String>
  ) : StatefulWorkflow<Boolean, Boolean, ChannelUpdate<String>, (Boolean) -> Unit>() {

    var subscriptions = 0
      private set

    var disposals = 0
      private set

    private val observable = subject
        .doOnSubscribe { subscriptions++ }
        .doOnDispose { disposals++ }

    override fun initialState(
      input: Boolean,
      snapshot: Snapshot?
    ): Boolean = input

    override fun compose(
      input: Boolean,
      state: Boolean,
      context: WorkflowContext<Boolean, ChannelUpdate<String>>
    ): (Boolean) -> Unit {
      if (state) {
        context.onNext(observable) { update -> emitOutput(update) }
      }
      return context.onEvent { subscribe -> enterState(subscribe) }
    }

    override fun snapshotState(state: Boolean) = Snapshot.EMPTY
  }

  private val subject = PublishSubject.create<String>()
  private val workflow = SubscriberWorkflow(subject)

  @Test fun `observable subscribes`() {
    workflow.testFromStart(false) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(0, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        setSubscribed(true)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        // Should still only be subscribed once.
        setSubscribed(true)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        setSubscribed(false)
      }

      assertEquals(1, workflow.subscriptions)
      assertEquals(1, workflow.disposals)
    }
  }

  @Test fun `observable unsubscribes`() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        setSubscribed(false)
      }

      assertEquals(1, workflow.subscriptions)
      assertEquals(1, workflow.disposals)
    }
  }

  @Test fun `observable subscribes only once across multiple composes`() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        // Should preserve subscriptions.
        setSubscribed(true)
      }

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
      }
    }
  }

  @Test fun `observable resubscribes`() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.disposals)
        setSubscribed(false)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(1, workflow.disposals)
        setSubscribed(true)
      }

      host.withNextRendering {
        assertEquals(2, workflow.subscriptions)
        assertEquals(1, workflow.disposals)
      }
    }
  }

  @Test fun `observable reports emissions`() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      subject.onNext("hello")
      assertEquals(Value("hello"), host.awaitNextOutput())
      assertFalse(host.hasOutput)

      subject.onNext("world")
      assertEquals(Value("world"), host.awaitNextOutput())
      assertFalse(host.hasOutput)
    }
  }

  @Test fun `observable reports close`() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      subject.onComplete()
      assertEquals(Closed, host.awaitNextOutput())
      assertFalse(host.hasOutput)

      // Assert no further close events are received.
      assertFailsWith<TimeoutCancellationException> {
        host.awaitNextOutput(timeoutMs = 1)
      }
    }
  }

  @Test fun `observable reports close after emission`() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      subject.onNext("foo")
      assertEquals(Value("foo"), host.awaitNextOutput())

      subject.onComplete()
      assertEquals(Closed, host.awaitNextOutput())
      assertFalse(host.hasOutput)
    }
  }

  @Test fun `observable reports error`() {
    assertFailsWith<IOException> {
      workflow.testFromStart(true) { host ->
        assertFalse(host.hasOutput)

        subject.onError(IOException("fail"))
        assertSame(Closed, host.awaitNextOutput())
        assertFalse(host.hasOutput)
      }
    }
  }
}
