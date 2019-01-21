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
package com.squareup.workflow.monitoring.tracing

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPoolMonitor
import com.squareup.workflow.WorkflowPoolMonitorEvent
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message.ReportEvent
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message.WriteTrace
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.withContext
import okio.Okio.sink
import okio.Sink
import java.io.File
import java.util.concurrent.TimeUnit.MICROSECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * A [WorkflowPoolMonitor] that collects events and can write out trace files compatible with
 * Chrome's `chrome://tracing` viewer.
 *
 * @param microsecondClock Function that returns the current time in **micro**seconds (_not_
 * milliseconds).
 */
class TracingWorkflowPoolMonitor(
  private val microsecondClock: () -> Long = systemMicrosecondClock,
  actorDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WorkflowPoolMonitor {

  private val actor =
    GlobalScope.actor<Message>(actorDispatcher, capacity = Channel.UNLIMITED) {
      val actor = TracingWorkflowPoolMonitorActor(ioDispatcher)
      for (message in channel) {
        actor.onMessage(message)
      }
    }

  /**
   * Formats all the recorded events into a trace file and writes it to [sink].
   *
   * The actual writes are done on the `IO` dispatcher, so this method can safely be called from
   * main UI threads.
   */
  suspend fun writeTraceFile(sink: Sink) {
    val onComplete = CompletableDeferred<Unit>()
    actor.offer(WriteTrace(sink, onComplete))
    onComplete.await()
  }

  suspend fun writeTraceFile(file: File) = withContext(ioDispatcher) {
    sink(file).use { sink ->
      writeTraceFile(sink)
    }
  }

  override fun report(
    pool: WorkflowPool,
    event: WorkflowPoolMonitorEvent
  ) {
    val timestamp = microsecondClock()
    actor.offer(ReportEvent(timestamp, pool, event))
  }

  fun dispose() {
    actor.close()
  }

  companion object {
    /**
     * Microsecond clock based on [System.nanoTime].
     */
    val systemMicrosecondClock = { MICROSECONDS.convert(System.nanoTime(), NANOSECONDS) }
  }
}
