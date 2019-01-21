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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.WorkflowPool.Type
import com.squareup.workflow.WorkflowPoolMonitorEvent
import com.squareup.workflow.WorkflowPoolMonitorEvent.Abandoned
import com.squareup.workflow.WorkflowPoolMonitorEvent.Finished
import com.squareup.workflow.WorkflowPoolMonitorEvent.Launched
import com.squareup.workflow.WorkflowPoolMonitorEvent.ReceivedEvent
import com.squareup.workflow.WorkflowPoolMonitorEvent.Registered
import com.squareup.workflow.WorkflowPoolMonitorEvent.RemovedFromPool
import com.squareup.workflow.WorkflowPoolMonitorEvent.StateChanged
import com.squareup.workflow.WorkflowPoolMonitorEvent.UpdateRequested
import com.squareup.workflow.monitoring.tracing.TraceEvent.Phase.DURATION_BEGIN
import com.squareup.workflow.monitoring.tracing.TraceEvent.Phase.DURATION_END
import com.squareup.workflow.monitoring.tracing.TraceEvent.Phase.INSTANT
import com.squareup.workflow.monitoring.tracing.TraceEvent.Scope.PROCESS
import com.squareup.workflow.monitoring.tracing.TraceEvent.Scope.THREAD
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message.ReportEvent
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message.WriteTrace
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import okio.Okio.buffer
import okio.Sink
import org.jetbrains.annotations.TestOnly
import java.util.LinkedList

/**
 * A typed actor that aggregates [TraceEvent]s and can write out formatted trace files.
 *
 * The use of the actor pattern allows all the work to be down on background threads, without
 * worrying about explicit synchronization. Event reports and write requests are just sent to the
 * actor's inbox as [Message]s, so event reporting has almost zero runtime cost on reporting thread.
 * Also, the IO performed during trace file writes are automatically performed on a background
 * thread.
 */
internal class TracingWorkflowPoolMonitorActor(
  private val ioDispatcher: CoroutineDispatcher
) {

  sealed class Message {
    /** @see TracingWorkflowPoolMonitor.report */
    data class ReportEvent(
      val timestamp: Long,
      val pool: WorkflowPool,
      val event: WorkflowPoolMonitorEvent
    ) : Message()

    /** @see TracingWorkflowPoolMonitor.writeTraceFile */
    data class WriteTrace(
      val sink: Sink,
      val onComplete: CompletableDeferred<Unit>
    ) : Message()
  }

  val adapter = buildMoshiAdapterForTraceFile()

  private val processIdsByWorkflowPool = mutableMapOf<WorkflowPool, Int>()
  private val threadIdsByType = mutableMapOf<Type<*, *, *>, Int>()
  private val launcherNamesByType = mutableMapOf<Type<*, *, *>, String>()
  private var nextProcessId = 0
  private var nextThreadId = 0
  private val traceEvents = LinkedList<TraceEvent>()

  @TestOnly
  internal fun peekTraceEvents(): List<TraceEvent> = traceEvents.toList()

  suspend fun onMessage(message: Message) {
    when (message) {

      is ReportEvent -> {
        traceEvents += createTraceEvent(message.timestamp, message.pool, message.event)
      }

      is WriteTrace -> {
        try {
          writeTraceEventsToSink(message.sink)
          message.onComplete.complete(Unit)
        } catch (e: Throwable) {
          message.onComplete.completeExceptionally(e)
        }
      }
    }
  }

  private fun createTraceEvent(
    timestamp: Long,
    pool: WorkflowPool,
    event: WorkflowPoolMonitorEvent
  ): TraceEvent {
    val template = TraceEvent(
        category = event.javaClass.simpleName,
        timestamp = timestamp,
        processId = pool.getProcessId(),
        threadId = -1,
        name = "",
        phase = INSTANT
    )
    return when (event) {

      is Registered -> template.copy(
          name = getAndCacheLauncherName(event.launcher, event.type),
          phase = INSTANT,
          scope = PROCESS,
          threadId = event.type.getThreadId(),
          args = mapOf(
              "type" to event.type.toString(),
              "launcher" to event.launcher.toString()
          )
      )

      // Starts a duration ended by Finished or Abandoned.
      is Launched -> template.copy(
          name = getRunningEventName(event.id),
          phase = DURATION_BEGIN,
          threadId = event.id.workflowType.getThreadId(),
          args = mapOf("initial state" to event.initialState.toString())
      )

      is UpdateRequested -> template.copy(
          name = getEventName(event.id) + " update requested",
          phase = INSTANT,
          scope = THREAD,
          threadId = event.id.workflowType.getThreadId(),
          args = mapOf("state" to event.state.toString())
      )

      is StateChanged -> template.copy(
          name = getEventName(event.id) + " state changed",
          phase = INSTANT,
          scope = THREAD,
          threadId = event.id.workflowType.getThreadId(),
          args = mapOf("new state" to event.newState.toString())
      )

      is ReceivedEvent -> template.copy(
          name = getEventName(event.id) + " received event",
          phase = INSTANT,
          scope = THREAD,
          threadId = event.id.workflowType.getThreadId(),
          args = mapOf("event" to event.event.toString())
      )

      // Ends the duration started by Launched (may also be ended by Abandoned).
      is Finished -> template.copy(
          name = getRunningEventName(event.id),
          phase = DURATION_END,
          threadId = event.id.workflowType.getThreadId(),
          args = mapOf("result" to event.result.toString())
      )

      // Ends the duration started by Launched (may also be ended by Finished).
      is Abandoned -> template.copy(
          name = getRunningEventName(event.id),
          phase = DURATION_END,
          threadId = event.id.workflowType.getThreadId()
      )

      is RemovedFromPool -> template.copy(
          name = "${getEventName(event.id)} removed from pool",
          phase = INSTANT,
          scope = THREAD,
          threadId = event.id.workflowType.getThreadId()
      )
    }
  }

  private suspend fun writeTraceEventsToSink(sink: Sink) {
    val traceFile = TraceFile(events = traceEvents)
    withContext(ioDispatcher) {
      val buffered = buffer(sink)
      try {
        adapter.toJson(buffered, traceFile)
      } finally {
        buffered.emit()
      }
    }
  }

  private fun WorkflowPool.getProcessId(): Int =
    processIdsByWorkflowPool.getOrPut(this) { nextProcessId++ }

  private fun Type<*, *, *>.getThreadId(): Int =
    threadIdsByType.getOrPut(this) { nextThreadId++ }

  private fun getAndCacheLauncherName(
    launcher: Launcher<*, *, *>,
    type: Type<*, *, *>
  ): String = launcher.javaClass.name.also { launcherNamesByType[type] = it } + " launched"

  private fun getRunningEventName(id: Id<*, *, *>) = "${getEventName(id)} running"
  private fun getEventName(id: Id<*, *, *>) =
    launcherNamesByType.getValue(id.workflowType) + "(${id.name})"
}

// Visible for testing.
internal fun buildMoshiAdapterForTraceFile(): JsonAdapter<TraceFile> {
  val moshi = Builder()
      .add(PhaseAdapter)
      .add(ScopeAdapter)
      .add(KotlinJsonAdapterFactory())
      .build()
  return moshi.adapter(TraceFile::class.java)
      .indent("  ")
}
