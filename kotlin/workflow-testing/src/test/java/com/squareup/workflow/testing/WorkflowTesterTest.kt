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
import com.squareup.workflow.internal.util.rethrowingUncaughtExceptions
import com.squareup.workflow.stateful
import com.squareup.workflow.stateless
import com.squareup.workflow.testing.WorkflowTestParams.StartMode.StartFromWorkflowSnapshot
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

  private class ExpectedException(message: String? = null) : RuntimeException(message)

  @Test fun `propagates exception when block throws`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart {
          throw ExpectedException()
        }
      }
    }
  }

  @Test fun `propagates exception when workflow throws from render`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      throw ExpectedException()
    }

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException>() {
        workflow.testFromStart {
          // Nothing to do.
        }
      }
    }
  }

  @Test fun `propagates exception when Job is cancelled in test block`() {
    val job = Job()
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart(context = job) {
          job.cancel(CancellationException(null, ExpectedException()))
        }
      }
    }
  }

  @Test fun `propagates close when Job is cancelled before starting`() {
    val job = Job().apply { cancel() }
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    rethrowingUncaughtExceptions {
      assertFailsWith<ClosedReceiveChannelException> {
        workflow.testFromStart(context = job) {
          awaitNextRendering()
        }
      }
    }
  }

  @Test fun `propagates cancellation when Job fails before starting`() {
    val job = Job().apply { cancel(CancellationException(null, ExpectedException())) }
    val workflow = Workflow.stateless<Unit, Unit, Unit> { }

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart(context = job) {
          // Nothing to do.
        }
      }
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

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart {
          // Nothing to do.
        }
      }
    }
  }

  @Test fun `propagates exception when workflow throws from snapshot state`() {
    val workflow = Workflow.stateful<Unit, Nothing, Unit>(
        initialState = { snapshot -> assertNull(snapshot) },
        render = {},
        snapshot = { throw ExpectedException() }
    )

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart {
          // Nothing to do.
        }
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
    val snapshot = Snapshot.of("dummy snapshot")

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart(
            WorkflowTestParams(startFrom = StartFromWorkflowSnapshot(snapshot))
        ) {
          // Nothing to do.
        }
      }
    }
  }

  @Test fun `propagates exception when worker throws`() {
    val deferred = CompletableDeferred<Unit>()
    deferred.completeExceptionally(ExpectedException())
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(deferred.asWorker()) { fail("Shouldn't get here.") }
    }

    rethrowingUncaughtExceptions {
      assertFailsWith<ExpectedException> {
        workflow.testFromStart {
          // Nothing to do.
        }
      }
    }
  }

  @Test fun `does nothing when no outputs observed`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}

    rethrowingUncaughtExceptions {
      workflow.testFromStart {
        // The workflow should never start.
      }
    }
  }

  @Test fun `workflow gets props from sendProps`() {
    val workflow = Workflow.stateless<String, Nothing, String> { props -> props }

    rethrowingUncaughtExceptions {
      workflow.testFromStart("one") {
        assertEquals("one", awaitNextRendering())
        sendProps("two")
        assertEquals("two", awaitNextRendering())
      }
    }
  }

  @Test fun `sendProps duplicate values all trigger render passes`() {
    var renders = 0
    val props = "props"
    val workflow = Workflow.stateless<String, Nothing, Unit> {
      renders++
    }

    rethrowingUncaughtExceptions {
      workflow.testFromStart(
          props,
          testParams = WorkflowTestParams(checkRenderIdempotence = false)
      ) {
        assertEquals(1, renders)

        sendProps(props)
        assertEquals(2, renders)

        sendProps(props)
        assertEquals(3, renders)
      }
    }
  }

  @Test fun `detects render side effects`() {
    var renderCount = 0
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderCount++
    }

    rethrowingUncaughtExceptions {
      workflow.testFromStart {
        assertEquals(2, renderCount)
      }
    }
  }

  @Test fun `detects render side effects disabled`() {
    var renderCount = 0
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      renderCount++
    }

    rethrowingUncaughtExceptions {
      workflow.testFromStart(testParams = WorkflowTestParams(checkRenderIdempotence = false)) {
        assertEquals(1, renderCount)
      }
    }
  }

  @Test fun `uncaught exceptions are suppressed when test body throws`() {
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      throw ExpectedException("render")
    }

    rethrowingUncaughtExceptions {
      val firstError = assertFailsWith<ExpectedException> {
        workflow.testFromStart {
          throw ExpectedException("test body")
        }
      }
      assertEquals("test body", firstError.message)
      val secondError = firstError.suppressed.single()
      assertTrue(secondError is ExpectedException)
      assertEquals("render", secondError.message)
    }
  }

  @Test fun `empty lambda still executes the workflow`() {
    var itLived = false
    val workflow = Workflow.stateless<Unit, Nothing, Unit> {
      itLived = true
    }
    workflow.testFromStart {
    }
    assertTrue(itLived)
  }
}
