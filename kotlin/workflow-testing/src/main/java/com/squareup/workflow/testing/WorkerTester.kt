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

import com.squareup.workflow.Worker
import com.squareup.workflow.Worker.Emitter
import com.squareup.workflow.testing.WorkflowTester.Companion.DEFAULT_TIMEOUT_MS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface WorkerTester<T> {

  /**
   * Suspends until the worker emits its next value, then returns it.
   */
  suspend fun nextOutput(): T

  /**
   * Throws an [AssertionError] if an output has been emitted since the last call to [nextOutput].
   */
  fun assertNoOutput()

  /**
   * Suspends until the worker emits an output or finishes.
   *
   * Throws an [AssertionError] if an output was emitted.
   */
  suspend fun assertFinished()

  /**
   * Throws an [AssertionError] immediately if the worker is finished.
   */
  fun assertNotFinished()

  /**
   * Suspends until the worker throws an exception, then returns it.
   */
  suspend fun getException(): Throwable

  /**
   * Cancels the worker and suspends until it's finished cancelling (joined).
   */
  suspend fun cancelWorker()
}

/**
 * Test a [Worker] by defining assertions on its output within [block].
 *
 * If you need to control time (e.g. your worker uses `delay()`), create a
 * [TestCoroutineDispatcher][kotlinx.coroutines.test.TestCoroutineDispatcher] and pass it as
 * [context].
 */
fun <T> Worker<T>.test(
  timeoutMs: Long = DEFAULT_TIMEOUT_MS,
  context: CoroutineContext = EmptyCoroutineContext,
  block: suspend WorkerTester<T>.() -> Unit
) {
  runBlocking(context) {
    val internalJob = CompletableDeferred<Job>()
    val channel: ReceiveChannel<T> = produce(SupervisorJob()) {
      internalJob.complete(coroutineContext[Job]!!)

      val emitter = object : Emitter<T> {
        override suspend fun emitOutput(output: T) {
          send(output)
        }
      }
      performWork(emitter)
    }

    val tester = object : WorkerTester<T> {
      override suspend fun nextOutput(): T = channel.receive()

      override fun assertNoOutput() {
        if (!channel.isEmpty) {
          throw AssertionError("Expected no output to have been emitted.")
        }
      }

      override suspend fun assertFinished() {
        try {
          val output = channel.receive()
          throw AssertionError("Expected Worker to finish, but emitted output: $output")
        } catch (e: ClosedReceiveChannelException) {
          // Expected.
        }
      }

      override fun assertNotFinished() {
        if (channel.isClosedForReceive) {
          throw AssertionError("Expected Worker to not be finished.")
        }
      }

      override suspend fun getException(): Throwable = try {
        val output = channel.receive()
        throw AssertionError("Expected Worker to throw an exception, but emitted output: $output")
      } catch (e: Throwable) {
        e
      }

      override suspend fun cancelWorker() {
        val job = internalJob.await()
        channel.cancel()
        job.join()
      }
    }

    withTimeout(timeoutMs) {
      // Wait for the worker coroutine to start, in case the worker is hot.
      // This is necessary because produce doesn't let us specify CoroutineStart.UNDISPATCHED.
      internalJob.await()
      block(tester)
    }
  }
}
