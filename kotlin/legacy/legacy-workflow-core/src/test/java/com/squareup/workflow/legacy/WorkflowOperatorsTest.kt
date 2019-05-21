/*
 * Copyright 2018 Square Inc.
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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkflowOperatorsTest {

  private class ExpectedException : RuntimeException()

  private val scope = CoroutineScope(Unconfined)

  @BeforeTest fun setUp() {
    assertTrue(scope.isActive)
  }

  @Test fun `adaptEvents works`() {
    val source = scope.workflow<Nothing, String, String> { _, events ->
      events.receive()
    }
    val withAdaptedEvents: Workflow<Nothing, Int, String> = source.adaptEvents {
      it.toString()
    }

    withAdaptedEvents.sendEvent(42)

    assertEquals("42", withAdaptedEvents.getCompleted())
  }

  @Test fun `adaptEvents send throws when transformer throws`() {
    val source = scope.workflow<Nothing, String, String> { _, events ->
      events.receive()
    }
    val withAdaptedEvents: Workflow<Nothing, Int, String> = source.adaptEvents {
      throw ExpectedException()
    }

    assertFailsWith<ExpectedException> { withAdaptedEvents.sendEvent(42) }
    assertFalse(source.isCompleted)
  }

  @Test fun `adaptEvents cancels upstream when transformed channel is cancelled`() {
    var sourceCancellation: Throwable? = null
    val source = scope.workflow<Nothing, String, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { continuation ->
        continuation.invokeOnCancellation { cause ->
          sourceCancellation = cause
        }
      }
    }
    val withAdaptedEvents: Workflow<Nothing, Int, String> = source.adaptEvents {
      it.toString()
    }

    withAdaptedEvents.cancel(CancellationException(null, ExpectedException()))

    assertTrue(sourceCancellation is CancellationException)
  }

  @Test fun `mapState works`() {
    val source = scope.workflow<Int, Unit, Unit> { state, events ->
      events.receive()
      state.send(1)
      events.receive()
      state.send(2)
      events.receive()
      state.send(3)
    }
    val withMappedStates = source.mapState {
      it * 2
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          withMappedStates.sendEvent(Unit)
          assertEquals(2, poll())
          withMappedStates.sendEvent(Unit)
          assertEquals(4, poll())
          withMappedStates.sendEvent(Unit)
          assertEquals(6, poll())
        }
  }

  @Test fun `mapState completes with error when transformer throws`() {
    val source = scope.workflow<Int, Unit, Unit> { state, events ->
      events.receive()
      state.send(42)
    }
    val withMappedStates: Workflow<Int, Unit, Unit> = source.mapState {
      throw ExpectedException()
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertFalse(withMappedStates.isCompleted)
          withMappedStates.sendEvent(Unit)
          assertFailsWith<ExpectedException> { poll() }
          // Exception is not sent through the result.
          assertEquals(Unit, withMappedStates.getCompleted())
        }
  }

  @Test fun `mapState forwards error from source`() {
    val source = scope.workflow<Int, Unit, Unit> { _, events ->
      events.receive()
      throw ExpectedException()
    }
    val withMappedStates = source.mapState {
      it * 2
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertFalse(withMappedStates.isCompleted)
          withMappedStates.sendEvent(Unit)
          assertFailsWith<ExpectedException> { poll() }
          assertFailsWith<ExpectedException> { withMappedStates.getCompleted() }
        }
  }

  @Test fun `mapState cancels upstream when transformed channel is cancelled`() {
    var sourceCancellation: Throwable? = null
    val source = scope.workflow<Nothing, Nothing, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { continuation ->
        continuation.invokeOnCancellation {
          sourceCancellation = it
        }
      }
    }
    val withMappedStates = source.mapState { it }

    assertNull(sourceCancellation)
    withMappedStates.cancel(CancellationException(null, ExpectedException()))
    assertTrue(sourceCancellation is CancellationException)
  }

  @Test fun `switchMapState forwards all transformed values when not preempted`() {
    val switchMapTrigger = Channel<Unit>()
    val source = scope.workflow<String, String, Unit> { state, events ->
      state.send(events.receive())
      state.send(events.receive())
    }
    val withMappedStates = source.switchMapState { state ->
      produce {
        state.forEach {
          send(it)
          switchMapTrigger.receive()
        }
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertNull(poll())

          withMappedStates.sendEvent("foo")
          assertEquals('f', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('o', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('o', poll())
          switchMapTrigger.offer(Unit)
          assertNull(poll())

          withMappedStates.sendEvent("bar")
          assertEquals('b', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('a', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('r', poll())
          switchMapTrigger.offer(Unit)
          assertNull(poll())
          assertTrue(isClosedForReceive)
        }
  }

  @Test fun `switchMapState cuts off previous transformed channel on new source value`() {
    val switchMapTrigger = Channel<Unit>()
    val source = scope.workflow<String, String, Unit> { state, events ->
      state.send(events.receive())
      state.send(events.receive())
    }
    val withMappedStates = source.switchMapState { state ->
      produce {
        state.forEach {
          send(it)
          switchMapTrigger.receive()
        }
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertNull(poll())

          withMappedStates.sendEvent("foo")
          assertEquals('f', poll())
          assertNull(poll())

          withMappedStates.sendEvent("bar")
          assertEquals('b', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('a', poll())
          switchMapTrigger.offer(Unit)
          assertEquals('r', poll())
          switchMapTrigger.offer(Unit)
          assertNull(poll())
          assertTrue(isClosedForReceive)
        }
  }

  @Test fun `switchMapState cancels previous transformed channel on new source value`() {
    val source = scope.workflow<String, String, Unit> { state, events ->
      state.send(events.receive())
      state.send(events.receive())
      suspendCancellableCoroutine { }
    }
    var mappedCancellation: Throwable? = null
    var mappedCancellationState: String? = null
    val withMappedStates = source.switchMapState { state ->
      Channel<Nothing>().apply {
        invokeOnClose { cause ->
          // Cause should be null.
          mappedCancellation = cause
          mappedCancellationState = state
        }
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertNull(mappedCancellationState)

          withMappedStates.sendEvent("foo")
          assertNull(mappedCancellationState)

          withMappedStates.sendEvent("bar")
          assertTrue(mappedCancellation is CancellationException)
          assertEquals("foo", mappedCancellationState)
        }
  }

  @Test fun `switchMapState conflates transformed values`() {
    val source = scope.workflow<String, String, Unit> { state, events ->
      state.send(events.receive())
      // Suspend forever.
      suspendCancellableCoroutine { }
    }
    val withMappedStates = source.switchMapState { state ->
      produce {
        // This state should get dropped.
        send("first $state")
        // Only this state should be seen.
        send("last $state")
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          withMappedStates.sendEvent("foo")
          assertEquals("last foo", poll())
        }
  }

  @Test fun `switchMapState completes with error when transformer throws`() {
    val source = scope.workflow<Int, Unit, Unit> { state, events ->
      events.receive()
      state.send(42)
    }
    val withMappedStates: Workflow<Int, Unit, Unit> = source.switchMapState {
      throw ExpectedException()
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertFalse(withMappedStates.isCompleted)
          withMappedStates.sendEvent(Unit)
          assertFails { runBlocking { receive() } }
              .also { error ->
                // Search up the cause chain for the expected exception, since multiple CancellationExceptions
                // may be chained together first.
                val causeChain = generateSequence(error) { it.cause }
                assertEquals(
                    1, causeChain.count { it is ExpectedException },
                    "Expected cancellation exception cause chain to include ExpectedException."
                )
              }
          // Exception is not sent through the result.
          assertEquals(Unit, withMappedStates.getCompleted())
        }
  }

  @Test fun `switchMapState state completes with error when transformed channel is cancelled`() {
    val source = scope.workflow<Int, Unit, Unit> { state, events ->
      events.receive()
      state.send(42)
    }
    val withMappedStates: Workflow<Int, Unit, Unit> = source.switchMapState {
      Channel<Int>().apply {
        cancel(CancellationException(null, ExpectedException()))
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertFalse(withMappedStates.isCompleted)
          withMappedStates.sendEvent(Unit)
          assertFails { runBlocking { receive() } }
              .also { error ->
                // Search up the cause chain for the expected exception, since multiple CancellationExceptions
                // may be chained together first.
                val causeChain = generateSequence(error) { it.cause }
                assertEquals(
                    1, causeChain.count { it is ExpectedException },
                    "Expected cancellation exception cause chain to include ExpectedException."
                )
              }
          // Exception is not sent through the result.
          assertEquals(Unit, withMappedStates.getCompleted())
        }
  }

  @Test fun `switchMapState forwards error from source`() {
    val source = scope.workflow<Int, Unit, Unit> { _, events ->
      events.receive()
      throw ExpectedException()
    }
    val withMappedStates: Workflow<Int, Unit, Unit> = source.switchMapState {
      produce {
        send(it * 2)
      }
    }

    withMappedStates.openSubscriptionToState()
        .consume {
          assertFalse(withMappedStates.isCompleted)
          withMappedStates.sendEvent(Unit)
          assertFailsWith<ExpectedException> { poll() }
          assertFailsWith<ExpectedException> { withMappedStates.getCompleted() }
        }
  }

  @Test fun `switchMapState cancels transformed channel when state channel cancelled`() {
    val transformedChannel = Channel<Nothing>()
    val source = scope.workflow<Unit, Unit, Nothing> { states, events ->
      events.receive()
      states.send(Unit)
      suspendCancellableCoroutine { }
    }
    val withMappedStates = source.switchMapState { transformedChannel }

    withMappedStates.openSubscriptionToState()
        .consume {
          withMappedStates.sendEvent(Unit)

          assertFalse(transformedChannel.isClosedForSend)
          this.cancel(CancellationException(null, ExpectedException()))
          assertTrue(transformedChannel.isClosedForSend)
        }
  }

  @Test fun `switchMapState cancels upstream workflow when workflow cancelled`() {
    var sourceCancellation: Throwable? = null
    val source = scope.workflow<Unit, Nothing, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { continuation ->
        continuation.invokeOnCancellation {
          sourceCancellation = it
        }
      }
    }
    val withMappedStates = source.switchMapState { produce { send(it) } }

    assertNull(sourceCancellation)
    withMappedStates.cancel(CancellationException(null, ExpectedException()))
    assertTrue(sourceCancellation is CancellationException)
  }

  @Test fun `mapResult works`() {
    val source = scope.workflow<Nothing, Unit, Int> { _, events ->
      events.receive()
      return@workflow 42
    }
    val withMappedResult = source.mapResult {
      it * 2
    }

    assertFalse(withMappedResult.isCompleted)
    withMappedResult.sendEvent(Unit)
    assertEquals(84, withMappedResult.getCompleted())
  }

  @Test fun `mapResult completes with error when transformer throws`() {
    val source = scope.workflow<Nothing, Unit, Int> { _, events ->
      events.receive()
      return@workflow 42
    }
    val withMappedResult: Workflow<Int, Unit, Unit> = source.mapResult {
      throw ExpectedException()
    }

    assertFalse(withMappedResult.isCompleted)
    withMappedResult.sendEvent(Unit)
    assertFailsWith<ExpectedException> { withMappedResult.getCompleted() }
  }

  @Test fun `mapResult forwards error from source`() {
    val source = scope.workflow<Nothing, Unit, Int> { _, events ->
      events.receive()
      throw ExpectedException()
    }
    val withMappedResult = source.mapResult {
      it * 2
    }

    assertFalse(withMappedResult.isCompleted)
    withMappedResult.sendEvent(Unit)
    assertFailsWith<ExpectedException> { withMappedResult.getCompleted() }
  }

  @Test fun `mapResult cancels upstream when transformed workflow is cancelled`() {
    var sourceCancellation: Throwable? = null
    val source = scope.workflow<Nothing, Nothing, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { continuation ->
        continuation.invokeOnCancellation {
          sourceCancellation = it
        }
      }
    }
    val withMappedResult = source.mapResult { it }

    assertNull(sourceCancellation)
    withMappedResult.cancel(CancellationException(null, ExpectedException()))
    assertTrue(sourceCancellation is CancellationException)
  }
}
