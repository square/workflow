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
import kotlinx.coroutines.CoroutineScope
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

/**
 * Launches a new coroutine that is a child of this node's scope, and calls
 * [com.squareup.workflow.Worker.run] from that coroutine. Returns a [ReceiveChannel] that
 * will emit everything from the worker. The channel will be closed when the flow completes.
 */
@UseExperimental(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> CoroutineScope.launchWorker(
  worker: Worker<T>,
  workerDiagnosticId: Long,
  workflowDiagnosticId: Long,
  diagnosticListener: WorkflowDiagnosticListener?
): ReceiveChannel<ValueOrDone<T>> = worker.run()
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
    .produceIn(this)

@UseExperimental(VeryExperimentalWorkflow::class)
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
