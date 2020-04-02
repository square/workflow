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
package com.squareup.workflow.internal

import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext

/**
 * Launches a new coroutine that is a child of this node's scope, and calls
 * [com.squareup.workflow.Worker.run] from that coroutine. Returns a [ReceiveChannel] that
 * will emit everything from the worker. The channel will be closed when the flow completes.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> CoroutineScope.launchWorker(
  worker: Worker<T>,
  key: String,
  workerDiagnosticId: Long,
  workflowDiagnosticId: Long,
  diagnosticListener: WorkflowDiagnosticListener?,
  workerContext: CoroutineContext
): ReceiveChannel<ValueOrDone<T>> = worker.runWithNullCheck()
    .wireUpDebugger(workerDiagnosticId, workflowDiagnosticId, diagnosticListener)
    .transformToValueOrDone()
    .catch { e ->
      // Workers that failed (as opposed to just cancelled) should have their failure reason
      // re-thrown from the workflow runtime. If we don't unwrap the cause here, they'll just
      // cause the runtime to cancel.
      val cancellationCause = e.unwrapCancellationCause()
      throw cancellationCause ?: e
    }
    // produceIn implicitly creates a buffer (it uses a Channel to bridge between contexts). This
    // operator is required to override the default buffer size.
    .buffer(RENDEZVOUS)
    .produceIn(createWorkerScope(worker, key, workerContext))

/**
 * In unit tests, if you use a mocking library to create a Worker, the run method will return null
 * even though the return type is non-nullable in Kotlin. Kotlin helps out with this by throwing an
 * NPE before before any kotlin code gets the null, but the NPE that it throws includes an almost
 * completely useless stacktrace and no other details.
 *
 * This method does an explicit null check and throws an exception with a more helpful message.
 *
 * See [#842](https://github.com/square/workflow/issues/842).
 */
@Suppress("USELESS_ELVIS")
private fun <T> Worker<T>.runWithNullCheck(): Flow<T> =
  run() ?: throw NullPointerException(
      "Worker $this returned a null Flow. " +
          "If this is a test mock, make sure you mock the run() method!"
  )

@OptIn(VeryExperimentalWorkflow::class)
private fun <T> Flow<T>.wireUpDebugger(
  workerDiagnosticId: Long,
  workflowDiagnosticId: Long,
  diagnosticListener: WorkflowDiagnosticListener?
): Flow<T> {
  // Only wire up debugging operators if we're actually debugging.
  if (diagnosticListener == null) return this
  return flow {
    try {
      collect { output ->
        diagnosticListener.onWorkerOutput(workerDiagnosticId, workflowDiagnosticId, output!!)
        emit(output)
      }
    } finally {
      diagnosticListener.onWorkerStopped(workerDiagnosticId, workflowDiagnosticId)
    }
  }
}

/**
 * Pretend we can use ReceiveChannel.onReceiveOrClosed.
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/1584 and
 * https://github.com/square/workflow/issues/626.
 */
internal class ValueOrDone<out T> private constructor(private val _value: Any?) {

  val isDone: Boolean get() = this === Done

  @Suppress("UNCHECKED_CAST")
  val value: T
    get() {
      check(!isDone)
      return _value as T
    }

  companion object {
    private val Done = ValueOrDone<Nothing>(null)

    fun <T> value(value: T): ValueOrDone<T> = ValueOrDone(value)
    fun done(): ValueOrDone<Nothing> = Done
  }
}

private fun <T> Flow<T>.transformToValueOrDone(): Flow<ValueOrDone<T>> = flow {
  collect {
    emit(ValueOrDone.value(it))
  }
  emit(ValueOrDone.done())
}

private fun CoroutineScope.createWorkerScope(
  worker: Worker<*>,
  key: String,
  workerContext: CoroutineContext
): CoroutineScope = this + CoroutineName(worker.debugName(key)) + Unconfined + workerContext

private fun Worker<*>.debugName(key: String) =
  toString().let { if (key.isBlank()) it else "$it:$key" }
