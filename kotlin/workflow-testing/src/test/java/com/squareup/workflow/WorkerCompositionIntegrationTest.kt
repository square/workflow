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

import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.testing.WorkerSink
import com.squareup.workflow.testing.testFromStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkerCompositionIntegrationTest {

  private class ExpectedException : RuntimeException()

  @Test fun `worker started`() {
    var started = false
    val worker = Worker.create<Unit> { started = true }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { props ->
      if (props) runningWorker(worker) { noAction() }
    }

    workflow.testFromStart(false) {
      assertFalse(started)
      sendProps(true)
      assertTrue(started)
    }
  }

  @Test fun `worker cancelled when dropped`() {
    var cancelled = false
    val worker = object : LifecycleWorker() {
      override fun onStopped() {
        cancelled = true
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { props ->
      if (props) runningWorker(worker) { noAction() }
    }

    workflow.testFromStart(true) {
      assertFalse(cancelled)
      sendProps(false)
      assertTrue(cancelled)
    }
  }

  @Test fun `worker only starts once over multiple renders`() {
    var starts = 0
    var stops = 0
    val worker = object : LifecycleWorker() {
      override fun onStarted() {
        starts++
      }

      override fun onStopped() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { runningWorker(worker) { noAction() } }

    workflow.testFromStart {
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendProps(Unit)
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendProps(Unit)
      assertEquals(1, starts)
      assertEquals(0, stops)
    }
  }

  @Test fun `worker restarts`() {
    var starts = 0
    var stops = 0
    val worker = object : LifecycleWorker() {
      override fun onStarted() {
        starts++
      }

      override fun onStopped() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { props ->
      if (props) runningWorker(worker) { noAction() }
    }

    workflow.testFromStart(false) {
      assertEquals(0, starts)
      assertEquals(0, stops)

      sendProps(true)
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendProps(false)
      assertEquals(1, starts)
      assertEquals(1, stops)

      sendProps(true)
      assertEquals(2, starts)
      assertEquals(1, stops)
    }
  }

  @Test fun `runningWorker gets output`() {
    val worker = WorkerSink<String>("")
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(worker) { action { setOutput(it) } }
    }

    workflow.testFromStart {
      assertFalse(this.hasOutput)

      worker.send("foo")

      assertEquals("foo", awaitNextOutput())
    }
  }

  @Test fun `runningWorker gets error`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(channel.asWorker()) { action { setOutput(it) } }
    }

    assertFailsWith<ExpectedException> {
      workflow.testFromStart {
        assertFalse(this.hasOutput)

        channel.cancel(CancellationException(null, ExpectedException()))

        awaitNextOutput()
      }
    }
  }

  @Test fun `onWorkerOutput does nothing when worker finished`() {
    val channel = Channel<Unit>()
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(channel.asWorker()) { fail("Expected handler to not be invoked.") }
    }

    workflow.testFromStart {
      channel.close()

      assertFailsWith<TimeoutCancellationException> {
        // There should never be any outputs, so this should timeout.
        awaitNextOutput(timeoutMs = 100)
      }
    }
  }

  // See https://github.com/square/workflow/issues/261.
  @Test fun `onWorkerOutput closes over latest state`() {
    val triggerOutput = WorkerSink<Unit>("")

    val incrementState = action<Int, Int> {
      nextState += 1
    }

    val workflow = Workflow.stateful<Int, Int, () -> Unit>(
        initialState = 0,
        render = { state ->
          runningWorker(triggerOutput) { action { setOutput(state) } }

          return@stateful { actionSink.send(incrementState) }
        }
    )

    workflow.testFromStart {
      triggerOutput.send(Unit)
      assertEquals(0, awaitNextOutput())

      awaitNextRendering()
          .invoke()
      triggerOutput.send(Unit)

      assertEquals(1, awaitNextOutput())

      awaitNextRendering()
          .invoke()
      triggerOutput.send(Unit)

      assertEquals(2, awaitNextOutput())
    }
  }

  @Test fun `runningWorker throws when output emitted`() {
    @Suppress("UNCHECKED_CAST")
    val worker = Worker.from { Unit } as Worker<Nothing>
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(worker)
    }

    assertFailsWith<AssertionError> {
      workflow.testFromStart {
        // Nothing to do.
      }
    }
  }

  @Test fun `runningWorker doesn't throw when worker finishes`() {
    // No-op worker, completes immediately.
    val worker = Worker.createSideEffect {}
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(worker)
    }

    workflow.testFromStart {
      assertFailsWith<TimeoutCancellationException> {
        awaitNextOutput(timeoutMs = 100)
      }
    }
  }

  @Test fun `worker coroutine uses test worker context`() {
    val worker = Worker.from { coroutineContext }
    val workflow = Workflow.stateless<Unit, CoroutineContext, Unit> {
      runningWorker(worker) { context -> action { setOutput(context) } }
    }
    val workerContext = CoroutineName("worker context")

    workflow.testFromStart(context = workerContext) {
      val actualWorkerContext = awaitNextOutput()
      assertEquals("worker context", actualWorkerContext[CoroutineName]!!.name)
    }
  }

  @Test fun `worker context job is ignored`() {
    val worker = Worker.from { coroutineContext }
    val leafWorkflow = Workflow.stateless<Unit, CoroutineContext, Unit> {
      runningWorker(worker) { context -> action { setOutput(context) } }
    }
    val workflow = Workflow.stateless<Unit, CoroutineContext, Unit> {
      renderChild(leafWorkflow) { action { setOutput(it) } }
    }
    val job: Job = Job()

    workflow.testFromStart(context = job) {
      val actualWorkerContext = awaitNextOutput()
      assertNotSame(job, actualWorkerContext[Job])
    }
  }

  @Test fun `worker context is used for workers`() {
    val worker = Worker.from { coroutineContext }
    val leafWorkflow = Workflow.stateless<Unit, CoroutineContext, Unit> {
      runningWorker(worker) { context -> action { setOutput(context) } }
    }
    val workflow = Workflow.stateless<Unit, CoroutineContext, Unit> {
      renderChild(leafWorkflow) { action { setOutput(it) } }
    }
    val dispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
      override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        Unconfined.isDispatchNeeded(context)

      override fun dispatch(
        context: CoroutineContext,
        block: Runnable
      ) = Unconfined.dispatch(context, block)
    }

    workflow.testFromStart(context = dispatcher) {
      val actualWorkerContext = awaitNextOutput()
      assertSame(dispatcher, actualWorkerContext[ContinuationInterceptor])
    }
  }
}
