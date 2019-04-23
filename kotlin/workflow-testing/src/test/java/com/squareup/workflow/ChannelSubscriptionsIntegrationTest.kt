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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.testing.testFromStart
import com.squareup.workflow.util.ChannelUpdate
import com.squareup.workflow.util.ChannelUpdate.Closed
import com.squareup.workflow.util.ChannelUpdate.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class ChannelSubscriptionsIntegrationTest {

  /**
   * Workflow that has a single Boolean state: whether to subscribe to [channel] or not.
   *
   * The initial value for the state is taken from the input. After that, the input is ignored.
   *
   * [render] returns a setter that will change the subscription state.
   */
  private class SubscriberWorkflow(
    private val channel: Channel<String>
  ) : StatelessWorkflow<Boolean, ChannelUpdate<String>, Unit>() {

    var subscriptions = 0
      private set

    var cancellations = 0
      private set

    init {
      channel.invokeOnClose { cancellations++ }
    }

    override fun render(
      input: Boolean,
      context: RenderContext<Nothing, ChannelUpdate<String>>
    ) {
      if (input) {
        context.onReceive({ subscribe() }) { update -> emitOutput(update) }
      }
    }

    private fun subscribe(): ReceiveChannel<String> {
      subscriptions++
      return channel
    }
  }

  private val channel = Channel<String>()
  private val workflow = SubscriberWorkflow(channel)

  @Test fun subscribes() {
    workflow.testFromStart(false) { host ->
      host.withNextRendering {
        assertEquals(0, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
      }

      host.sendInput(true)

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
      }

      // Should still only be subscribed once.
      host.sendInput(true)

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        host.sendInput(false)

        assertEquals(1, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
      }
    }
  }

  @Test fun unsubscribes() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        host.sendInput(false)

        assertEquals(1, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
      }
    }
  }

  @Test fun `subscribes only once across multiple composes`() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        // Should preserve subscriptions.
        host.sendInput(true)
      }

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
      }
    }
  }

  @Test fun resubscribes() {
    workflow.testFromStart(true) { host ->
      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(0, workflow.cancellations)
        host.sendInput(false)
      }

      host.withNextRendering {
        assertEquals(1, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
        host.sendInput(true)
      }

      host.withNextRendering {
        assertEquals(2, workflow.subscriptions)
        assertEquals(1, workflow.cancellations)
      }
    }
  }

  @Test fun `reports emissions`() {
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

  @Test fun `reports close`() {
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

  @Test fun `reports close after emission`() {
    workflow.testFromStart(true) { host ->
      assertFalse(host.hasOutput)

      channel.sendBlocking("foo")
      assertEquals(Value("foo"), host.awaitNextOutput())

      channel.close()
      assertEquals(Closed, host.awaitNextOutput())
      assertFalse(host.hasOutput)
    }
  }

  @Test fun `reports error`() {
    assertFailsWith<IOException> {
      workflow.testFromStart(true) { host ->
        assertFalse(host.hasOutput)

        // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
        @Suppress("DEPRECATION")
        channel.cancel(IOException("fail"))
        assertSame(Closed, host.awaitNextOutput())
        assertFalse(host.hasOutput)
      }
    }
  }

  // See https://github.com/square/workflow/issues/261.
  @Test fun `onReceive closes over latest state`() {
    val trigger = Channel<Unit>()
    val workflow = object : StatefulWorkflow<Unit, Int, Int, (Unit) -> Unit>() {
      override fun initialState(
        input: Unit,
        snapshot: Snapshot?,
        scope: CoroutineScope
      ) = 0

      override fun render(
        input: Unit,
        state: Int,
        context: RenderContext<Int, Int>
      ): (Unit) -> Unit {
        context.onReceive({ trigger }) { emitOutput(state) }
        return context.onEvent { enterState(state + 1) }
      }

      override fun snapshotState(state: Int) = Snapshot.EMPTY
    }

    workflow.testFromStart { tester ->
      trigger.offer(Unit)
      assertEquals(0, tester.awaitNextOutput())

      tester.awaitNextRendering()
          .invoke(Unit)
      trigger.offer(Unit)

      assertEquals(1, tester.awaitNextOutput())

      tester.awaitNextRendering()
          .invoke(Unit)
      trigger.offer(Unit)

      assertEquals(2, tester.awaitNextOutput())
    }
  }
}
