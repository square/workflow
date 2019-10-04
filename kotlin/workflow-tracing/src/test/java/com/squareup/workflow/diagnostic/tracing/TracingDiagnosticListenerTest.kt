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
import com.squareup.tracing.TraceEvent
import com.squareup.tracing.TraceLogger
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.asWorker
import com.squareup.workflow.launchWorkflowIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.buffer
import okio.source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ClockMark
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@UseExperimental(ExperimentalCoroutinesApi::class)
class TracingDiagnosticListenerTest {

  @Test fun `golden value`() {
    val buffer = Buffer()
    val memoryStats = object : MemoryStats {
      override fun freeMemory(): Long = 42
      override fun totalMemory(): Long = 43
    }
    val scope = CoroutineScope(Unconfined)
    val encoder = TraceEncoder(
        scope = scope,
        start = ZeroClockMark,
        ioDispatcher = Unconfined,
        sinkProvider = { buffer }
    )
    val listener = TracingDiagnosticListener(memoryStats = memoryStats) { _ -> encoder }
    val props = (0..100).asFlow()
    val renderings = launchWorkflowIn(scope, TestWorkflow, props) { session ->
      session.diagnosticListener = listener
      session.renderingsAndSnapshots.map { it.rendering }
    }

    runBlocking {
      renderings.takeWhile { it != "final" }
          .collect()
    }
    scope.cancel()

    val expected = TracingDiagnosticListenerTest::class.java
        .getResourceAsStream("expected_trace_file.txt")
        .source()
        .buffer()
    assertEquals(expected.readUtf8(), buffer.readUtf8())
  }
}

private class RecordingTraceLogger : TraceLogger {

  private val _events = mutableListOf<List<TraceEvent>>()
  val events: List<List<TraceEvent>> get() = _events

  override fun log(event: TraceEvent) = log(listOf(event))

  override fun log(eventBatch: List<TraceEvent>) {
    _events += eventBatch
  }
}

private object TestWorkflow : StatefulWorkflow<Int, String, String, String>() {

  private val channel = Channel<String>(UNLIMITED)

  fun triggerWorker(value: String) {
    channel.offer(value)
  }

  override fun initialState(
    props: Int,
    snapshot: Snapshot?
  ): String = "initial"

  override fun onPropsChanged(
    old: Int,
    new: Int,
    state: String
  ): String {
    if (old == 2 && new == 3) triggerWorker("fired!")
    return if (old == 0 && new == 1) "changed state" else state
  }

  override fun render(
    props: Int,
    state: String,
    context: RenderContext<String, String>
  ): String {
    if (props == 0) return "initial"
    if (props in 1..6) context.renderChild(TestWorkflow, 0) { bubbleUp(it) }
    if (props in 4..5) context.renderChild(TestWorkflow, props = 1, key = "second") { bubbleUp(it) }
    if (props in 2..3) context.runningWorker(channel.asWorker(false)) { bubbleUp(it) }

    return if (props > 10) "final" else "rendering"
  }

  override fun snapshotState(state: String): Snapshot = Snapshot.EMPTY

  private fun bubbleUp(output: String) = WorkflowAction<String, String>({ "action" }) { output }
}

@UseExperimental(ExperimentalTime::class)
private object ZeroClockMark : ClockMark() {
  override fun elapsedNow(): Duration = Duration.ZERO
}
