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

package com.squareup.workflow.testing

import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.asWorker
import com.squareup.workflow.runningWorker
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkflowTesterTest {

  private class ExpectedException : RuntimeException()

  @Test fun `propagates exception when block throws`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        throw ExpectedException()
      }
    }
  }

  @Test fun `propagates exception when workflow throws from render`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      throw ExpectedException()
    }

    workflow.testFromStart {
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `propagates exception when Job is cancelled in test block`() {
    val job = Job()
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    workflow.testFromStart(context = job) {
      job.cancel(CancellationException(null, ExpectedException()))
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `propagates close when Job is cancelled before starting`() {
    val job = Job().apply { cancel() }
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    workflow.testFromStart(context = job) {
      assertTrue(awaitFailure() is ClosedReceiveChannelException)
    }
  }

  @Test fun `propagates cancellation when Job failes before starting`() {
    val job = Job().apply { cancel(CancellationException(null, ExpectedException())) }
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    workflow.testFromStart(context = job) {
      assertTrue(awaitFailure() is ExpectedException)
    }
  }

  @Test fun `propagates exception when workflow throws from initial state`() {
    val workflow = Workflow.stateful<Unit, Unit, Nothing, Unit>(
        initialState = { _, snapshot ->
          assertNull(snapshot)
          throw ExpectedException()
        },
        render = { _, _ -> fail() },
        snapshot = { fail() }
    )

    workflow.testFromStart {
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `propagates exception when workflow throws from snapshot state`() {
    val workflow = Workflow.stateful<Unit, Nothing, Unit>(
        initialState = { snapshot -> assertNull(snapshot) },
        render = {},
        snapshot = { throw ExpectedException() }
    )

    workflow.testFromStart {
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `propagates exception when workflow throws from restore state`() {
    val workflow = Workflow.stateful<Unit, Nothing, Unit>(
        initialState = { snapshot ->
          if (snapshot != null) {
            throw ExpectedException()
          }
        },
        render = {},
        snapshot = { Snapshot.EMPTY }
    )

    // Get a valid snapshot (can't use Snapshot.EMPTY).
    val snapshot = workflow.testFromStart {
      awaitNextSnapshot()
    }

    workflow.testFromStart(snapshot) {
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `propagates exception when worker throws`() {
    val deferred = CompletableDeferred<Unit>()
    deferred.completeExceptionally(ExpectedException())
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(deferred.asWorker()) { fail("Shouldn't get here.") }
    }

    workflow.testFromStart {
      awaitFailure()
          .let { error ->
            val causeChain = generateSequence(error) { it.cause }
            assertEquals(1, causeChain.count { it is ExpectedException })
          }
    }
  }

  @Test fun `does nothing when no outputs observed`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}

    workflow.testFromStart {
      // The workflow should never start.
    }
  }

  @Test fun `workflow gets props from sendProps`() {
    val workflow = Workflow.stateless<String, Nothing, String> { props -> props }

    workflow.testFromStart("one") {
      assertEquals("one", awaitNextRendering())
      sendProps("two")
      assertEquals("two", awaitNextRendering())
    }
  }

  @Test fun `sendProps duplicate values all trigger render passes`() {
    var renders = 0
    val props = "props"
    val workflow = Workflow.stateless<String, Nothing, Unit> { renders++ }

    workflow.testFromStart(props) {
      assertEquals(1, renders)

      sendProps(props)
      assertEquals(2, renders)

      sendProps(props)
      assertEquals(3, renders)
    }
  }
}
