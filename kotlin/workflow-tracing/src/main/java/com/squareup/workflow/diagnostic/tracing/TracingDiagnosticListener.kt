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
 * TODO kdoc
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
 * TODO kdoc
 */
@Suppress("FunctionName")
fun TracingDiagnosticListener(
  name: String = "",
  encoderProvider: (workflowScope: CoroutineScope) -> TraceEncoder
): TracingDiagnosticListener = TracingDiagnosticListener { workflowScope, type ->
  val encoder = encoderProvider(workflowScope)
  val processName = name.ifEmpty { type }
  encoder.createLogger(
      processName = processName,
      threadName = "Profiling"
  )
}

/**
 * TODO kdoc
 */
@UseExperimental(VeryExperimentalWorkflow::class)
class TracingDiagnosticListener(
  private val loggerProvider: (
    workflowScope: CoroutineScope,
    workflowType: String
  ) -> TraceLogger
) : WorkflowDiagnosticListener {

  /**
   * [NONE] is fine here because it will get initialized by [onRuntimeStarted] and there's no
   * race conditions.
   */
  private var logger: TraceLogger? = null
  private var gcDetector: GcDetector? = null
  private val runtime = Runtime.getRuntime()

  private val workflowNamesById = mutableMapOf<Long, String>()
  private val workerDescriptionsById = mutableMapOf<Long, String>()

  override fun onRuntimeStarted(
    workflowScope: CoroutineScope,
    rootWorkflowType: String
  ) {
    logger = loggerProvider(workflowScope, rootWorkflowType)

    // Log garbage collections in case they correlate with unusually long render times.
    gcDetector = GcDetector {
      logger?.log(
          listOf(
              Instant(
                  name = "GC detected",
                  scope = GLOBAL,
                  category = "system",
                  args = mapOf(
                      "freeMemory" to runtime.freeMemory(),
                      "totalMemory" to runtime.totalMemory()
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
    val freeMemory = runtime.freeMemory()
    val usedMemory = runtime.totalMemory() - freeMemory
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
