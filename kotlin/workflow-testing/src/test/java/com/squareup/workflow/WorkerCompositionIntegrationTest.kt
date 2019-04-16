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
import kotlinx.coroutines.channels.Channel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerCompositionIntegrationTest {

  private class ExpectedException : RuntimeException()

  @Test fun `worker started`() {
    var started = false
    val worker = Worker.create<Unit> { started = true }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(false) { tester ->
      assertFalse(started)
      tester.sendInput(true)
      assertTrue(started)
    }
  }

  @Test fun `worker cancelled when dropped`() {
    var cancelled = false
    val worker = object : LifecycleWorker() {
      override suspend fun onCancelled() {
        cancelled = true
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(true) { tester ->
      assertFalse(cancelled)
      tester.sendInput(false)
      assertTrue(cancelled)
    }
  }

  @Test fun `worker only starts once over multiple renders`() {
    var starts = 0
    var stops = 0
    val worker = object : LifecycleWorker() {
      override suspend fun onStarted() {
        starts++
      }

      override suspend fun onCancelled() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { _, context ->
      context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart { tester ->
      assertEquals(1, starts)
      assertEquals(0, stops)

      tester.sendInput(Unit)
      assertEquals(1, starts)
      assertEquals(0, stops)

      tester.sendInput(Unit)
      assertEquals(1, starts)
      assertEquals(0, stops)
    }
  }

  @Test fun `worker restarts`() {
    var starts = 0
    var stops = 0
    val worker = object : LifecycleWorker() {
      override suspend fun onStarted() {
        starts++
      }

      override suspend fun onCancelled() {
        stops++
      }
    }
    val workflow = Workflow.stateless<Boolean, Nothing, Unit> { input, context ->
      if (input) context.onWorkerOutput(worker) { noop() }
    }

    workflow.testFromStart(false) { tester ->
      assertEquals(0, starts)
      assertEquals(0, stops)

      tester.sendInput(true)
      assertEquals(1, starts)
      assertEquals(0, stops)

      tester.sendInput(false)
      assertEquals(1, starts)
      assertEquals(1, stops)

      tester.sendInput(true)
      assertEquals(2, starts)
      assertEquals(1, stops)
    }
  }

  @Test fun `onWorkerOutputOrFinished gets output`() {
    val channel = Channel<String>(capacity = 1)
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart { tester ->
      assertFalse(tester.hasOutput)

      channel.offer("foo")

      assertEquals(Output("foo"), tester.awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets finished`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart { tester ->
      assertFalse(tester.hasOutput)

      channel.close()

      assertEquals(Finished, tester.awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets finished after value`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart { tester ->
      assertFalse(tester.hasOutput)

      channel.offer("foo")

      assertEquals(Output("foo"), tester.awaitNextOutput())

      channel.close()

      assertEquals(Finished, tester.awaitNextOutput())
    }
  }

  @Test fun `onWorkerOutputOrFinished gets error`() {
    val channel = Channel<String>()
    val workflow = Workflow.stateless<OutputOrFinished<String>, Unit> { context ->
      context.onWorkerOutputOrFinished(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart { tester ->
      assertFalse(tester.hasOutput)

      // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
      @Suppress("DEPRECATION")
      channel.cancel(ExpectedException())

      assertFailsWith<ExpectedException> {
        tester.awaitNextOutput()
      }
    }
  }

  @Test fun `onWorkerOutput throws when worker finished`() {
    val channel = Channel<Unit>()
    val workflow = Workflow.stateless<Unit, Unit> { context ->
      context.onWorkerOutput(channel.asWorker()) { emitOutput(it) }
    }

    workflow.testFromStart { tester ->
      channel.close()
      val error = tester.awaitFailure()
      assertTrue(error is NoSuchElementException)
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

    workflow.testFromStart { tester ->
      triggerOutput.offer(Unit)
      assertEquals(0, tester.awaitNextOutput())

      tester.awaitNextRendering()
          .invoke(Unit)
      triggerOutput.offer(Unit)

      assertEquals(1, tester.awaitNextOutput())

      tester.awaitNextRendering()
          .invoke(Unit)
      triggerOutput.offer(Unit)

      assertEquals(2, tester.awaitNextOutput())
    }
  }
}
