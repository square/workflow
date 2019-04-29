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

import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Worker.OutputOrFinished.Finished
import com.squareup.workflow.Worker.OutputOrFinished.Output
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import com.squareup.workflow.WorkflowAction.Companion.noop
import com.squareup.workflow.testing.testFromStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class WorkerCompositionIntegrationTest {

  private class ExpectedException : RuntimeException()

  @Test fun `worker started`() {
    var started = false
    val worker = Worker.create<Unit> { started = true }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(false) {
      assertFalse(started)
      sendInput(true)
      assertTrue(started)
    }
  }

  @Test fun `worker cancelled when dropped`() {
    var cancelled = false
    val worker = object : LifecycleWorker() {
      override fun onCancelled() {
        cancelled = true
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(true) {
      assertFalse(cancelled)
      sendInput(false)
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

      override fun onCancelled() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { _, context ->
      context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart {
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendInput(Unit)
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendInput(Unit)
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

      override fun onCancelled() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(false) {
      assertEquals(0, starts)
      assertEquals(0, stops)

      sendInput(true)
      assertEquals(1, starts)
      assertEquals(0, stops)

      sendInput(false)
      assertEquals(1, starts)
      assertEquals(1, stops)

      sendInput(true)
      assertEquals(2, starts)
      assertEquals(1, stops)
    }
  }

  @Test fun `onWorkerOutputOrFinished gets output`() {
    val channel = Channel<String>(capacity = 1)
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart {
      assertFalse(this.hasOutput)

      channel.offer("foo")

      assertEquals(Output("foo"), awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets finished`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart {
      assertFalse(this.hasOutput)

      channel.close()

      assertEquals(Finished, awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets finished after value`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart {
      assertFalse(this.hasOutput)

      channel.offer("foo")

      assertEquals(Output("foo"), awaitNextOutput())

      channel.close()

      assertEquals(Finished, awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets error`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart {
      assertFalse(this.hasOutput)

      // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
      @Suppress("DEPRECATION")
      channel.cancel(ExpectedException())

      assertFailsWith<ExpectedException> {
        awaitNextOutput()
      }
    }
  }

  @Test fun `onWorkerOutput does nothing when worker finished`() {
    val channel = Channel<Unit>()
    val workflow = Workflow.stateless<Unit, Unit> { context ->
      context.onWorkerOutput(channel.asWorker()) { fail("Expected handler to not be invoked.") }
    }

    workflow.testFromStart {
      channel.close()

      assertFailsWith<TimeoutCancellationException> {
        // There should never be any outputs, so this should timeout.
        awaitNextOutput(timeoutMs = 1000)
      }
    }
  }

  // See https://github.com/square/workflow/issues/261.
  @Test fun `onWorkerOutput closes over latest state`() {
    val triggerOutput = Channel<Unit>()
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
        context.onWorkerOutput(triggerOutput.asWorker()) { emitOutput(state) }
        return context.onEvent { enterState(state + 1) }
      }

      override fun snapshotState(state: Int) = Snapshot.EMPTY
    }

    workflow.testFromStart {
      triggerOutput.offer(Unit)
      assertEquals(0, awaitNextOutput())

      awaitNextRendering()
          .invoke(Unit)
      triggerOutput.offer(Unit)

      assertEquals(1, awaitNextOutput())

      awaitNextRendering()
          .invoke(Unit)
      triggerOutput.offer(Unit)

      assertEquals(2, awaitNextOutput())
    }
  }
}
