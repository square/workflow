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
package com.squaruep.workflow.v2

import com.squareup.workflow.legacy.Snapshot
import com.squareup.workflow.v2.ChannelUpdate
import com.squareup.workflow.v2.ChannelUpdate.Closed
import com.squareup.workflow.v2.ChannelUpdate.Value
import com.squareup.workflow.v2.Workflow
import com.squareup.workflow.v2.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.v2.WorkflowAction.Companion.enterState
import com.squareup.workflow.v2.WorkflowContext
import com.squareup.workflow.v2.onReceive
import com.squareup.workflow.v2.testing.testFromStart
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.sendBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.fail

class ChannelSubscriptionsIntegrationTest {

  private val channel = Channel<String>()
  private val workflow = SubscriberWorkflow(channel)

  @Test fun subscribes() {
    workflow.testFromStart(false) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(0, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        setSubscribed(true)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        // Should still only be subscribed once.
        setSubscribed(true)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        setSubscribed(false)
      }

      assertEquals(1, workflow.subscriptions)
      assertEquals(1, workflow.cancellations)
    }
  }

  @Test fun unsubscribes() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        setSubscribed(false)
      }

      assertEquals(1, workflow.subscriptions)
      assertEquals(1, workflow.cancellations)
    }
  }

  @Test fun subscribesOnlyOnceAcrossMultipleComposes() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        // Should preserve subscriptions.
        setSubscribed(true)
      }

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
      }
    }
  }

  @Test fun resubscribes() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        setSubscribed(false)
      }

      host.withNextRendering { setSubscribed ->
        assertEquals(1, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
        setSubscribed(true)
      }

      host.withNextRendering {
        assertEquals(2, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
      }
    }
  }

  @Test fun reportsEmissions() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      channel.sendBlocking("hello")
      assertEquals(Value("hello"), host.awaitNextOutput())
      assertFalse(host.hasOutput)

      channel.sendBlocking("world")
      assertEquals(Value("world"), host.awaitNextOutput())
      assertFalse(host.hasOutput)
    }
  }

  @Test fun reportsClose() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      channel.close()
      assertEquals(Closed, host.awaitNextOutput())
      assertFalse(host.hasOutput)

      // Assert no further close events are received.
      assertFailsWith<TimeoutCancellationException> {
        host.awaitNextOutput(timeoutMs = 1)
      }
    }
  }

  @Test fun reportsCloseAfterEmission() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      channel.sendBlocking("foo")
      assertEquals(Value("foo"), host.awaitNextOutput())

      channel.close()
      assertEquals(Closed, host.awaitNextOutput())
      assertFalse(host.hasOutput)
    }
  }

  @Test fun reportsError() {
    assertFailsWith<IOException> {
      workflow.testFromStart(true) { host ->
        assertFalse(host.hasOutput)

        channel.cancel(IOException("fail"))
        assertSame(Closed, host.awaitNextOutput())
        assertFalse(host.hasOutput)
      }
    }
  }

  /**
   * Workflow that has a single Boolean state: whether to subscribe to [channel] or not.
   *
   * The initial value for the state is taken from the input. After that, the input is ignored.
   *
   * [compose] returns a setter that will change the subscription state.
   */
  private class SubscriberWorkflow(
    private val channel: Channel<String>
  ) : Workflow<Boolean, Boolean, ChannelUpdate<String>, (setSubscribed: Boolean) -> Unit> {

    var subscriptions = 0
      private set

    var cancellations = 0
      private set

    init {
      channel.invokeOnClose { cancellations++ }
    }

    override fun initialState(input: Boolean): Boolean = input

    override fun compose(
      input: Boolean,
      state: Boolean,
      context: WorkflowContext<Boolean, ChannelUpdate<String>>
    ): (Boolean) -> Unit {
      if (state) {
        context.onReceive({ subscribe() }) { update -> emitOutput(update) }
      }
      return context.makeSink { subscribe -> enterState(subscribe) }
    }

    override fun snapshotState(state: Boolean) = Snapshot.EMPTY
    override fun restoreState(snapshot: Snapshot): Boolean = fail()

    private fun subscribe(): ReceiveChannel<String> {
      subscriptions++
      return channel
    }
  }
}
