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

package com.squareup.workflow.internal

import com.squareup.workflow.Worker
import com.squareup.workflow.asWorker
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorkersTest {

  @Test fun `launchWorker propagates backpressure`() {
    val channel = Channel<String>()
    val worker = channel.asWorker()
    // Used to assert ordering.
    val counter = AtomicInteger(0)

    runBlocking {
      val workerOutputs = launchWorker(worker)

      launch(start = UNDISPATCHED) {
        assertEquals(0, counter.getAndIncrement())
        channel.send("a")
        assertEquals(1, counter.getAndIncrement())
        channel.send("b")
        assertEquals(3, counter.getAndIncrement())
        channel.close()
        assertEquals(4, counter.getAndIncrement())
      }
      yield()
      assertEquals(2, counter.getAndIncrement())

      assertEquals("a", workerOutputs.poll()!!.value)
      yield()
      assertEquals(5, counter.getAndIncrement())

      assertEquals("b", workerOutputs.poll()!!.value)
      yield()
      assertEquals(6, counter.getAndIncrement())

      // Cancel the worker so we can exit this loop.
      workerOutputs.cancel()
    }
  }

  @Test fun `launchWorker emits diagnostic events`() {
    val channel = Channel<String>()
    val worker = Worker.create<String> { emitAll(channel) }
    val workerId = 0L
    val workflowId = 1L
    val listener = RecordingDiagnosticListener()

    runBlocking {
      val outputs = launchWorker(worker, "", workerId, workflowId, listener, EmptyCoroutineContext)

      // Start event is sent by WorkflowNode.
      yield()
      assertTrue(listener.consumeEvents().isEmpty())

      channel.send("foo")
      outputs.receive()

      assertEquals("onWorkerOutput(0, 1, foo)", listener.consumeNextEvent())

      channel.close()
      yield()

      assertEquals("onWorkerStopped(0, 1)", listener.consumeNextEvent())
      // Read the last event so the scope can complete.
      assertTrue(outputs.receive().isDone)
    }
  }

  @Test fun `launchWorker emits done when complete immediately`() {
    val channel = Channel<String>(capacity = 1)

    runBlocking {
      val workerOutputs = launchWorker(channel.asWorker())
      assertTrue(workerOutputs.isEmpty)

      channel.close()
      assertTrue(workerOutputs.receive().isDone)
    }
  }

  @Test fun `launchWorker emits done when complete after sending`() {
    val channel = Channel<String>(capacity = 1)

    runBlocking {
      val workerOutputs = launchWorker(channel.asWorker())
      assertTrue(workerOutputs.isEmpty)

      channel.send("foo")
      assertEquals("foo", workerOutputs.receive().value)

      channel.close()
      assertTrue(workerOutputs.receive().isDone)
    }
  }

  @Test fun `launchWorker does not emit done when failed`() {
    val channel = Channel<String>(capacity = 1)

    runBlocking {
      // Needed so that cancelling the channel doesn't cancel our job, which means receive will
      // throw the JobCancellationException instead of the actual channel failure.
      supervisorScope {
        val workerOutputs = launchWorker(channel.asWorker())
        assertTrue(workerOutputs.isEmpty)

        channel.close(ExpectedException())
        assertFailsWith<ExpectedException> { workerOutputs.receive() }
      }
    }
  }

  @Test fun `launchWorker completes after emitting done`() {
    val channel = Channel<String>(capacity = 1)

    runBlocking {
      val workerOutputs = launchWorker(channel.asWorker())
      channel.close()
      assertTrue(workerOutputs.receive().isDone)

      assertTrue(channel.isClosedForReceive)
    }
  }

  /**
   * This should be impossible, since the return type is non-nullable. However it is very easy to
   * accidentally create a mock using libraries like Mockito in unit tests that return null Flows.
   */
  @Test fun `launchWorker throws when flow is null`() {
    val nullFlowWorker = NullFlowWorker()

    val error = runBlocking {
      assertFailsWith<NullPointerException> {
        launchWorker(nullFlowWorker)
      }
    }

    assertEquals(
        "Worker NullFlowWorker.toString returned a null Flow. " +
            "If this is a test mock, make sure you mock the run() method!",
        error.message
    )
  }

  @Test fun `launchWorker coroutine is named without key`() {
    val output = runBlocking {
      launchWorker(CoroutineNameWorker)
          .consume { receive() }
          .value
    }

    assertEquals("CoroutineNameWorker.toString", output)
  }

  @Test fun `launchWorker coroutine is named with key`() {
    val output = runBlocking {
      launchWorker(CoroutineNameWorker, key = "foo")
          .consume { receive() }
          .value
    }

    assertEquals("CoroutineNameWorker.toString:foo", output)
  }

  @Test fun `launchWorker dispatcher is unconfined`() {
    val worker = Worker.from { coroutineContext[ContinuationInterceptor] }

    runBlocking {
      val interceptor = launchWorker(worker)
          .consume { receive() }
          .value

      assertSame(Dispatchers.Unconfined, interceptor)
    }
  }

  private fun <T> CoroutineScope.launchWorker(
    worker: Worker<T>,
    key: String = ""
  ) = launchWorker(
      worker,
      key = key,
      workerDiagnosticId = 0,
      workflowDiagnosticId = 0,
      diagnosticListener = null,
      workerContext = EmptyCoroutineContext
  )

  private class ExpectedException : RuntimeException()

  private object CoroutineNameWorker : Worker<String> {
    override fun run(): Flow<String> = flow {
      val nameElement = coroutineContext[CoroutineName] as CoroutineName
      emit(nameElement.name)
    }

    override fun toString(): String = "CoroutineNameWorker.toString"
  }
}
