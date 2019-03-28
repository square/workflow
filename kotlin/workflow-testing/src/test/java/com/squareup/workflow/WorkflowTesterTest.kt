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
package com.squareup.workflow

import com.squareup.workflow.testing.testFromStart
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.JobCancellationException
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class WorkflowTesterTest {

  @Test fun `propagates exception when block throws`() {
    val workflow = Workflow.stateless<Unit, Unit> { }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        throw ExpectedException()
      }
    }
  }

  @Test fun `propagates exception when workflow throws from compose`() {
    val workflow = Workflow.stateless<Unit, Unit> {
      throw ExpectedException()
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `propagates exception when Job is cancelled in test block`() {
    val job = Job()
    val workflow = Workflow.stateless<Unit, Unit> { }

    assertFailsWith<ClosedReceiveChannelException> {
      workflow.testFromStart(context = job) {
        job.cancel()
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `propagates exception when Job is cancelled before starting`() {
    val job = Job().apply { cancel() }
    val workflow = Workflow.stateless<Unit, Unit> { }

    assertFailsWith<JobCancellationException> {
      workflow.testFromStart(context = job) {
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `propagates exception when workflow throws from initial state`() {
    val workflow = object : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {
      override fun initialState(input: Unit) {
        throw ExpectedException()
      }

      override fun compose(
        input: Unit,
        state: Unit,
        context: WorkflowContext<Unit, Nothing>
      ) {
        fail()
      }

      override fun snapshotState(state: Unit): Snapshot {
        fail()
      }

      override fun restoreState(snapshot: Snapshot) {
        fail()
      }
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `propagates exception when workflow throws from snapshot state`() {
    val workflow = object : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {
      override fun initialState(input: Unit) {
        // Noop
      }

      override fun compose(
        input: Unit,
        state: Unit,
        context: WorkflowContext<Unit, Nothing>
      ) {
        // Noop
      }

      override fun snapshotState(state: Unit): Snapshot {
        throw ExpectedException()
      }

      override fun restoreState(snapshot: Snapshot) {
        fail()
      }
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `propagates exception when workflow throws from restore state`() {
    val workflow = object : StatefulWorkflow<Unit, Unit, Nothing, Unit>() {
      override fun initialState(input: Unit) {
      }

      override fun compose(
        input: Unit,
        state: Unit,
        context: WorkflowContext<Unit, Nothing>
      ) {
      }

      override fun snapshotState(state: Unit): Snapshot = Snapshot.EMPTY

      override fun restoreState(snapshot: Snapshot) {
        throw ExpectedException()
      }
    }

    // Get a valid snapshot (can't use Snapshot.EMPTY).
    val snapshot = workflow.testFromStart {
      it.awaitNextSnapshot()
    }
    assertFailsWith<ExpectedException> {
      workflow.testFromStart(snapshot) {
        // Workflow should never start.
      }
    }
  }

  @Test fun `propagates exception when subscription throws`() {
    val deferred = CompletableDeferred<Unit>()
    deferred.completeExceptionally(ExpectedException())
    val workflow = Workflow.stateless<Unit, Unit> {
      it.onDeferred(deferred) { fail("Shouldn't get here.") }
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        it.awaitNextRendering()
      }
    }
  }

  @Test fun `does nothing when no outputs observed`() {
    val workflow = Workflow.stateless<Unit, Unit> {}

    workflow.testFromStart {
      // The workflow should never start.
    }
  }

  private class ExpectedException : RuntimeException()
}
