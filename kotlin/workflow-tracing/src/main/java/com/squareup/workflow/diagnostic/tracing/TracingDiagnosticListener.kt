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
package com.squareup.workflow.diagnostic.tracing

import com.squareup.tracing.TraceEncoder
import com.squareup.tracing.TraceEvent.AsyncDurationBegin
import com.squareup.tracing.TraceEvent.AsyncDurationEnd
import com.squareup.tracing.TraceEvent.Counter
import com.squareup.tracing.TraceEvent.DurationBegin
import com.squareup.tracing.TraceEvent.DurationEnd
import com.squareup.tracing.TraceEvent.Instant
import com.squareup.tracing.TraceEvent.Instant.InstantScope.GLOBAL
import com.squareup.tracing.TraceEvent.Instant.InstantScope.PROCESS
import com.squareup.tracing.TraceEvent.ObjectCreated
import com.squareup.tracing.TraceEvent.ObjectDestroyed
import com.squareup.tracing.TraceEvent.ObjectSnapshot
import com.squareup.tracing.TraceLogger
import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.CoroutineScope
import okio.buffer
import okio.sink
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE

/**
 * A [WorkflowDiagnosticListener] that generates a trace file that can be viewed in Chrome by
 * visiting `chrome://tracing`.
 *
 * @param file The [File] to write the trace to.
 * @param name If non-empty, will be used to set the "process name" in the trace file. If empty,
 * the workflow type is used for the process name.
 */
@Suppress("FunctionName")
fun TracingDiagnosticListener(
  file: File,
  name: String = ""
): TracingDiagnosticListener = TracingDiagnosticListener(name) { workflowScope ->
  TraceEncoder(workflowScope) {
    file.sink()
        .buffer()
  }
}

/**
 * A [WorkflowDiagnosticListener] that generates a trace file that can be viewed in Chrome by
 * visiting `chrome://tracing`.
 *
 * @param name If non-empty, will be used to set the "process name" in the trace file. If empty,
 * the workflow type is used for the process name.
 * @param encoderProvider A function that returns a [TraceEncoder] that will be used to write trace
 * events. The function gets the [CoroutineScope] that the workflow runtime is running in.
 */
@Suppress("FunctionName")
fun TracingDiagnosticListener(
  name: String = "",
  memoryStats: MemoryStats = RuntimeMemoryStats,
  encoderProvider: (workflowScope: CoroutineScope) -> TraceEncoder
): TracingDiagnosticListener =
  TracingDiagnosticListener(memoryStats = memoryStats) { workflowScope, type ->
    provideLogger(name, workflowScope, type, encoderProvider)
  }

internal fun provideLogger(
  name: String,
  workflowScope: CoroutineScope,
  workflowType: String,
  encoderProvider: (workflowScope: CoroutineScope) -> TraceEncoder
): TraceLogger {
  val encoder = encoderProvider(workflowScope)
  val processName = name.ifEmpty { workflowType }
  return encoder.createLogger(
      processName = processName,
      threadName = "Profiling"
  )
}

/**
 * A [WorkflowDiagnosticListener] that generates a trace file that can be viewed in Chrome by
 * visiting `chrome://tracing`.
 *
 * @constructor The primary constructor is internal so that it can inject [GcDetector] for tests.
 */
@OptIn(VeryExperimentalWorkflow::class)
class TracingDiagnosticListener internal constructor(
  private val memoryStats: MemoryStats,
  private val gcDetectorConstructor: GcDetectorConstructor,
  private val loggerProvider: (
    workflowScope: CoroutineScope,
    workflowType: String
  ) -> TraceLogger
) : WorkflowDiagnosticListener {

  /**
   * A [WorkflowDiagnosticListener] that generates a trace file that can be viewed in Chrome by
   * visiting `chrome://tracing`.
   *
   * @param loggerProvider A function that returns a [TraceLogger] that will be used to write trace
   * events. The function gets the [CoroutineScope] that the workflow runtime is running in, as well
   * as the same workflow type description passed to [WorkflowDiagnosticListener.onWorkflowStarted].
   */
  constructor(
    memoryStats: MemoryStats = RuntimeMemoryStats,
    loggerProvider: (
      workflowScope: CoroutineScope,
      workflowType: String
    ) -> TraceLogger
  ) : this(memoryStats, ::GcDetector, loggerProvider)

  /**
   * [NONE] is fine here because it will get initialized by [onRuntimeStarted] and there's no
   * race conditions.
   */
  private var logger: TraceLogger? = null
  private var gcDetector: GcDetector? = null

  private val workflowNamesById = mutableMapOf<Long, String>()
  private val workerDescriptionsById = mutableMapOf<Long, String>()

  override fun onRuntimeStarted(
    workflowScope: CoroutineScope,
    rootWorkflowType: String
  ) {
    logger = loggerProvider(workflowScope, rootWorkflowType)

    // Log garbage collections in case they correlate with unusually long render times.
    gcDetector = gcDetectorConstructor {
      logger?.log(
          listOf(
              Instant(
                  name = "GC detected",
                  scope = GLOBAL,
                  category = "system",
                  args = mapOf(
                      "freeMemory" to memoryStats.freeMemory(),
                      "totalMemory" to memoryStats.totalMemory()
                  )
              ),
              createMemoryEvent()
          )
      )
    }
  }

  override fun onRuntimeStopped() {
    gcDetector?.stop()
  }

  override fun onBeforeRenderPass(props: Any?) {
    logger?.log(
        listOf(
            DurationBegin(
                name = "Render Pass",
                category = "rendering",
                args = mapOf("props" to props.toString())
            ),
            createMemoryEvent()
        )
    )
  }

  override fun onAfterRenderPass(rendering: Any?) {
    logger?.log(
        listOf(
            DurationEnd(
                name = "Render Pass",
                category = "rendering",
                args = mapOf("rendering" to rendering.toString())
            ),
            createMemoryEvent()
        )
    )
  }

  override fun onWorkflowStarted(
    workflowId: Long,
    parentId: Long?,
    workflowType: String,
    key: String,
    initialProps: Any?,
    initialState: Any?,
    restoredFromSnapshot: Boolean
  ) {
    val keyPart = if (key.isEmpty()) "" else ":$key"
    val name = "${workflowType.takeLastWhile { it != '.' }}$keyPart (${workflowId.toHex()})"
    workflowNamesById[workflowId] = name
    logger?.log(
        listOf(
            AsyncDurationBegin(
                id = "workflow",
                name = name,
                category = "workflow",
                args = mapOf(
                    "workflowId" to workflowId.toHex(),
                    "initialProps" to initialProps.toString(),
                    "initialState" to initialState.toString(),
                    "restoredFromSnapshot" to restoredFromSnapshot,
                    "parent" to workflowNamesById[parentId]
                )
            ),
            ObjectCreated(
                id = workflowId,
                objectType = name
            )
        )
    )
  }

  override fun onWorkflowStopped(workflowId: Long) {
    val name = workflowNamesById.getValue(workflowId)
    logger?.log(
        listOf(
            AsyncDurationEnd(
                id = "workflow",
                name = name,
                category = "workflow"
            ),
            ObjectDestroyed(
                id = workflowId,
                objectType = name
            )
        )
    )
    workflowNamesById -= workflowId
  }

  override fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) {
    val parentName = workflowNamesById.getValue(parentWorkflowId)
    workerDescriptionsById[workerId] = description
    logger?.log(
        AsyncDurationBegin(
            id = "workflow",
            name = "Worker: ${workerId.toHex()}",
            category = "workflow",
            args = mapOf(
                "parent" to parentName,
                "key" to key,
                "description" to description
            )
        )
    )
  }

  override fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) {
    val description = workerDescriptionsById.getValue(workerId)
    logger?.log(
        AsyncDurationEnd(
            id = "workflow",
            name = "Worker: ${workerId.toHex()}",
            category = "workflow",
            args = mapOf("description" to description)
        )
    )
    workerDescriptionsById -= workerId
  }

  override fun onBeforeWorkflowRendered(
    workflowId: Long,
    props: Any?,
    state: Any?
  ) {
    val name = workflowNamesById.getValue(workflowId)
    logger?.log(
        DurationBegin(
            name,
            args = mapOf(
                "workflowId" to workflowId.toHex(),
                "props" to props.toString(),
                "state" to state.toString()
            ),
            category = "rendering"
        )
    )
  }

  override fun onAfterWorkflowRendered(
    workflowId: Long,
    rendering: Any?
  ) {
    val name = workflowNamesById.getValue(workflowId)
    logger?.log(
        DurationEnd(
            name,
            args = mapOf("rendering" to rendering.toString()),
            category = "rendering"
        )
    )
  }

  override fun onBeforeSnapshotPass() {
    logger?.log(DurationBegin(name = "Snapshot"))
  }

  override fun onAfterSnapshotPass() {
    logger?.log(DurationEnd(name = "Snapshot"))
  }

  override fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) {
    val name = workflowNamesById.getValue(workflowId)
    logger?.log(
        Instant(
            name = "Sink received: $name",
            category = "update",
            args = mapOf("action" to action.toString())
        )
    )
  }

  override fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) {
    val parentName = workflowNamesById.getValue(parentWorkflowId)
    val description = workerDescriptionsById.getValue(workerId)
    logger?.log(
        Instant(
            name = "Worker output: $parentName",
            category = "update",
            args = mapOf(
                "workerId" to workerId.toHex(),
                "description" to description,
                "output" to output.toString()
            )
        )
    )
  }

  override fun onPropsChanged(
    workflowId: Long?,
    oldProps: Any?,
    newProps: Any?,
    oldState: Any?,
    newState: Any?
  ) {
    val name = workflowNamesById[workflowId] ?: "{root}"
    logger?.log(
        Instant(
            name = "Props changed: $name",
            args = mapOf(
                "oldProps" to oldProps.toString(),
                "newProps" to if (oldProps == newProps) "{no change}" else newProps.toString(),
                "oldState" to oldState.toString(),
                "newState" to if (oldState == newState) "{no change}" else newState.toString()
            )
        )
    )
  }

  override fun onWorkflowAction(
    workflowId: Long,
    action: WorkflowAction<*, *>,
    oldState: Any?,
    newState: Any?,
    output: Any?
  ) {
    val name = workflowNamesById.getValue(workflowId)

    logger?.log(
        listOf(
            Instant(
                name = "WorkflowAction: $name",
                category = "update",
                scope = PROCESS,
                args = mapOf(
                    "action" to action.toString(),
                    "oldState" to oldState.toString(),
                    "newState" to if (oldState == newState) "{no change}" else newState.toString(),
                    "output" to output.toString()
                )
            ),
            ObjectSnapshot(
                id = workflowId,
                objectType = name,
                snapshot = newState.toString()
            )
        )
    )
  }

  private fun createMemoryEvent(): Counter {
    val freeMemory = memoryStats.freeMemory()
    val usedMemory = memoryStats.totalMemory() - freeMemory
    return Counter(
        name = "used/free memory",
        series = mapOf(
            // This map is ordered. The stacked chart is shown in reverse order so it looks like a
            // typical memory usage graph.
            "usedMemory" to usedMemory,
            "freeMemory" to freeMemory
        )
    )
  }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.toHex() = toString(16)
