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

import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Launcher
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
import com.squareup.workflow.monitoring.tracing.TracingWorkflowPoolMonitorActor.Message.ReportEvent
import com.squareup.workflow.workflowType
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.runBlocking
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private const val TIMESTAMP = 42L

class TracingWorkflowPoolMonitorActorTest {
  private val pool = WorkflowPool()
  private val actor = TracingWorkflowPoolMonitorActor(Dispatchers.Unconfined)

  @Test fun `Registered trace event`() {
    assertTraceEquals(
        Registered(NoopLauncher.workflowType, NoopLauncher),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher launched",
            category = "Registered",
            phase = INSTANT,
            scope = PROCESS,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf(
                "type" to NoopLauncher.typeString,
                "launcher" to "NoopLauncher"
            )
        )
    )
  }

  @Test fun `Launched trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        Launched(NoopLauncher.makeId("name"), Foo),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) running",
            category = "Launched",
            phase = DURATION_BEGIN,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf("initial state" to Foo.toString())
        )
    )
  }

  @Test fun `UpdateRequested trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        UpdateRequested(NoopLauncher.makeId("name"), Foo),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) update requested",
            category = "UpdateRequested",
            phase = INSTANT,
            scope = THREAD,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf("state" to Foo.toString())
        )
    )
  }

  @Test fun `StateChanged trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        StateChanged(NoopLauncher.makeId("name"), Foo),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) state changed",
            category = "StateChanged",
            phase = INSTANT,
            scope = THREAD,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf("new state" to Foo.toString())
        )
    )
  }

  @Test fun `ReceivedEvent trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        ReceivedEvent(NoopLauncher.makeId("name"), Bar),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) received event",
            category = "ReceivedEvent",
            phase = INSTANT,
            scope = THREAD,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf("event" to Bar.toString())
        )
    )
  }

  @Test fun `Finished trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        Finished(NoopLauncher.makeId("name"), Baz),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) running",
            category = "Finished",
            phase = DURATION_END,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0,
            args = mapOf("result" to Baz.toString())
        )
    )
  }

  @Test fun `Abandoned trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        Abandoned(NoopLauncher.makeId("name")),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) running",
            category = "Abandoned",
            phase = DURATION_END,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0
        )
    )
  }

  @Test fun `RemovedFromPool trace event`() {
    getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    assertTraceEquals(
        RemovedFromPool(NoopLauncher.makeId("name")),
        TraceEvent(
            name = "com.squareup.workflow.monitoring.tracing.NoopLauncher(name) removed from pool",
            category = "RemovedFromPool",
            phase = INSTANT,
            scope = THREAD,
            timestamp = TIMESTAMP,
            processId = 0,
            threadId = 0
        )
    )
  }

  @Test fun `processIds assigned to pools then reused`() {
    val pool1 = WorkflowPool()
    val pool2 = WorkflowPool()
    val monitorEvent = Registered(NoopLauncher.workflowType, NoopLauncher)
    assertEquals(0, getTraceEvent(monitorEvent, pool1).processId)
    assertEquals(1, getTraceEvent(monitorEvent, pool2).processId)
    assertEquals(0, getTraceEvent(monitorEvent, pool1).processId)
  }

  @Test fun `threadIds assigned by type`() {
    // Launcher with a different type than NoopLauncher.
    val launcherDifferentType = object : Launcher<String, String, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ): Workflow<String, String, String> = fail("Shouldn't be called.")
    }
    val launcherWithSameType = object : Launcher<Foo, Bar, Baz> {
      override fun launch(
        initialState: Foo,
        workflows: WorkflowPool
      ): Workflow<Foo, Bar, Baz> = fail("Shouldn't be called.")
    }
    val event1 = getTraceEvent(Registered(NoopLauncher.workflowType, NoopLauncher))
    val event2 =
      getTraceEvent(Registered(launcherDifferentType.workflowType, launcherDifferentType))
    val event3 = getTraceEvent(Registered(launcherWithSameType.workflowType, launcherWithSameType))

    assertEquals(0, event1.threadId)
    assertEquals(1, event2.threadId)
    assertEquals(0, event3.threadId)
  }

  @Test fun `TraceFile adapter golden value`() {
    val adapter = buildMoshiAdapterForTraceFile()
    val buffer = Buffer()
    adapter.toJson(
        buffer,
        TraceFile(
            events = listOf(
                TraceEvent(
                    name = "name",
                    category = "category",
                    phase = INSTANT,
                    scope = PROCESS,
                    timestamp = TIMESTAMP,
                    processId = 43,
                    threadId = 44,
                    args = mapOf("foo" to "bar", "baz" to 32)
                )
            ),
            otherData = mapOf("other" to "data")
        )
    )

    assertEquals(
        """
          {
            "traceEvents": [
              {
                "name": "name",
                "cat": "category",
                "ph": "i",
                "ts": 42,
                "pid": 43,
                "tid": 44,
                "s": "p",
                "args": {
                  "foo": "bar",
                  "baz": 32
                }
              }
            ],
            "otherData": {
              "other": "data"
            }
          }
        """.trimIndent(), buffer.readUtf8()
    )
  }

  private fun assertTraceEquals(
    event: WorkflowPoolMonitorEvent,
    trace: TraceEvent
  ) {
    assertEquals(trace, getTraceEvent(event))
  }

  private fun getTraceEvent(
    event: WorkflowPoolMonitorEvent,
    pool: WorkflowPool = this.pool
  ): TraceEvent = runBlocking {
    actor.onMessage(ReportEvent(TIMESTAMP, pool, event))
    return@runBlocking actor.peekTraceEvents()
        .last()
  }
}

private object Foo
private object Bar
private object Baz

private object NoopLauncher : Launcher<Foo, Bar, Baz> {

  val typeString = workflowType.toString()

  override fun launch(
    initialState: Foo,
    workflows: WorkflowPool
  ): Workflow<Foo, Bar, Baz> {
    fail("Shouldn't be called.")
  }

  fun makeId(name: String = "") = Id(name, NoopLauncher.workflowType)

  override fun toString(): String = "NoopLauncher"
}
