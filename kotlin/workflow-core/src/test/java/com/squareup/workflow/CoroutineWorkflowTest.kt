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
package com.squareup.workflow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoroutineWorkflowTest : CoroutineScope {

  private val testContext = TestCoroutineContext()
  override val coroutineContext = Unconfined + testContext

  @Test fun `single state`() {
    val workflow = workflow<String, Nothing, Unit> { state, _ ->
      state.send("foo")
      suspendCoroutine<Unit> { }
    }
    workflow.openSubscriptionToState()
        .consume {
          testContext.triggerActions()
          assertEquals("foo", poll())
        }
  }

  @Test fun `states are conflated`() {
    val workflow = workflow<String, Nothing, Unit> { state, _ ->
      state.send("foo")
      state.send("bar")
      suspendCoroutine<Unit> { }
    }
    workflow.openSubscriptionToState()
        .consume {
          testContext.triggerActions()
          assertEquals("bar", poll())
        }
  }

  @Test fun `state channel is closed when block returns`() {
    val workflow = workflow<String, Nothing, Unit> { _, _ -> }
    workflow.openSubscriptionToState()
        .consume {
          testContext.triggerActions()
          assertNull(poll())
          assertTrue(isClosedForReceive)
        }
  }

  @Test fun `state channel is closed when block throws`() {
    val workflow = workflow<String, Nothing, Unit> { _, _ ->
      throw ExpectedException
    }
    workflow.openSubscriptionToState()
        .consume {
          testContext.triggerActions()
          assertFailsWith<ExpectedException> { poll() }
          assertTrue(isClosedForReceive)
        }
  }

  @Test fun `state channel is closed when workflow cancelled`() {
    val workflow = workflow<String, Nothing, Unit> { _, _ ->
      suspendCancellableCoroutine<Unit> { }
    }
    workflow.openSubscriptionToState()
        .consume {
          workflow.cancel()
          testContext.triggerActions()
          assertFailsWith<CancellationException> { poll() }
        }
  }

  @Test fun `events are buffered`() {
    lateinit var eventsFromWorkflow: ReceiveChannel<Int>
    val workflow = workflow<Nothing, Int, Unit> { _, events ->
      eventsFromWorkflow = events
      suspendCoroutine<Unit> {}
    }
    testContext.triggerActions()

    for (i in 0..99) workflow.sendEvent(i)

    for (i in 0..99) {
      assertEquals(i, eventsFromWorkflow.poll())
    }
  }

  @Test fun `accepts events after completion`() {
    val workflow = workflow<Nothing, Unit, Unit> { _, _ -> }
    testContext.triggerActions()
    assertTrue(workflow.isCompleted)

    workflow.sendEvent(Unit)
  }

  @Test fun `successful result is reported`() {
    val workflow = workflow<Nothing, Nothing, String> { _, _ ->
      "finished"
    }
    testContext.triggerActions()
    assertEquals("finished", workflow.getCompleted())
  }

  @Test fun `error result is reported`() {
    val workflow = workflow<Nothing, Nothing, Nothing> { _, _ ->
      throw ExpectedException
    }
    testContext.triggerActions()
    assertFailsWith<ExpectedException> { workflow.getCompleted() }
  }

  @Test fun `cancelled result is reported`() {
    val workflow = workflow<Nothing, Nothing, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { }
    }
    workflow.cancel()
    testContext.triggerActions()
    assertFailsWith<CancellationException> { workflow.getCompleted() }
  }

  @Test fun `coroutine uses context from scope`() {
    val scope = CoroutineScope(coroutineContext + CoroutineName("foo"))
    lateinit var innerName: String
    scope.workflow<Nothing, Nothing, Unit> { _, _ ->
      innerName = coroutineContext[CoroutineName]!!.name
    }
    testContext.triggerActions()
    assertEquals("foo", innerName)
  }

  @Test fun `coroutine uses context from argument`() {
    val context = CoroutineName("foo")
    lateinit var innerName: String
    workflow<Nothing, Nothing, Unit>(context) { _, _ ->
      innerName = coroutineContext[CoroutineName]!!.name
    }
    testContext.triggerActions()
    assertEquals("foo", innerName)
  }

  @Test fun `sending on cancelled event channel doesn't throw`() {
    val workflow = workflow<Nothing, Unit, Unit> { _, events ->
      events.cancel()
    }
    testContext.triggerActions()

    workflow.sendEvent(Unit)
  }

  @Test fun `block gets original cancellation reason`() {
    lateinit var cancelReason: Throwable
    val workflow = workflow<Nothing, Nothing, Nothing> { _, _ ->
      suspendCancellableCoroutine<Nothing> { continuation ->
        continuation.invokeOnCancellation {
          cancelReason = it!!
        }
      }
    }
    testContext.triggerActions()
    workflow.cancel(ExpectedException)

    assertTrue(cancelReason is CancellationException)
    assertEquals(ExpectedException, cancelReason.cause)
  }

  private companion object {
    object ExpectedException : RuntimeException()
  }
}
