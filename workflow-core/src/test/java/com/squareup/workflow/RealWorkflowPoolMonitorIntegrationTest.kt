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
package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.WorkflowPoolMonitorEvent.Abandoned
import com.squareup.workflow.WorkflowPoolMonitorEvent.Finished
import com.squareup.workflow.WorkflowPoolMonitorEvent.Launched
import com.squareup.workflow.WorkflowPoolMonitorEvent.ReceivedEvent
import com.squareup.workflow.WorkflowPoolMonitorEvent.Registered
import com.squareup.workflow.WorkflowPoolMonitorEvent.RemovedFromPool
import com.squareup.workflow.WorkflowPoolMonitorEvent.StateChanged
import com.squareup.workflow.WorkflowPoolMonitorEvent.UpdateRequested
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.toChannel
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that the correct events are reported to the [WorkflowPoolMonitor] when [RealWorkflowPool]
 * goes about its business.
 */
@Suppress("DeferredResultUnused")
class RealWorkflowPoolMonitorIntegrationTest : CoroutineScope {

  private val monitor = TestWorkflowPoolMonitor()
  private val pool = RealWorkflowPool(monitor)

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Unconfined

  @BeforeTest fun setup() {
    assertTrue(monitor.rawEvents.isEmpty())
  }

  @AfterTest fun `tear down`() {
    assertTrue("all events have the same pool") {
      monitor.rawEvents.all { it.pool === pool }
    }
  }

  @Test fun `emits Registered on registration`() {
    val launcher = object : Launcher<String, Unit, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ) = workflow<String, Unit, String> { _, _ -> initialState }
    }

    pool.register(launcher)

    monitor.assertEvents(Registered(launcher.workflowType, launcher))
  }

  @Test fun `emits Launched on workflow creation`() {
    val launcher = object : Launcher<String, Unit, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ) = workflow<String, Unit, String> { _, _ -> suspendCoroutine<Nothing> { } }
    }
    val handle = WorkflowPool.handle(launcher::class, "start")
    pool.register(launcher)

    pool.workflowUpdate(handle)

    monitor.assertEventsEndsWith(Launched(handle.id, "start"))
  }

  @Test fun `emits Launched on worker creation`() {
    val worker = worker<String, Unit> { suspendCoroutine<Nothing> { } }

    pool.workerResult(worker, "start")

    assertTrue(Launched(worker.workflowType.makeWorkflowId(), "start") in monitor.events)
  }

  @Test fun `emits UpdateRequested on update for workflow`() {
    val launcher = object : Launcher<String, Unit, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ) = workflow<String, Unit, String> { _, _ -> suspendCoroutine<Nothing> { } }
    }
    val handle = WorkflowPool.handle(launcher::class, "start")
    pool.register(launcher)

    pool.workflowUpdate(handle)

    assertTrue(UpdateRequested(handle.id, handle.state) in monitor.events)
  }

  @Test fun `emits UpdateRequested on update for worker`() {
    val worker = worker<String, Unit> { suspendCoroutine<Nothing> { } }

    pool.workerResult(worker, "start")

    assertTrue(UpdateRequested(worker.workflowType.makeWorkflowId(), "start") in monitor.events)
  }

  @Test fun `emits UpdateRequested before first workflow Launch`() {
    val launcher = object : Launcher<String, Unit, String> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ) = workflow<String, Unit, String> { _, _ -> suspendCoroutine<Nothing> { } }
    }
    val handle = WorkflowPool.handle(launcher::class, "start")
    pool.register(launcher)

    pool.workflowUpdate(handle)

    monitor.assertEventsEndsWith(
        UpdateRequested(handle.id, handle.state),
        Launched(handle.id, handle.state)
    )
  }

  @Test fun `initialization event order for worker`() {
    val worker = worker<String, Unit> { suspendCoroutine<Nothing> { } }
    val id = worker.workflowType.makeWorkflowId()

    pool.workerResult(worker, "start")

    val (update, register, launch) = monitor.events
    update as UpdateRequested
    register as Registered
    launch as Launched
    assertEquals(UpdateRequested(id, "start"), update)
    assertEquals(worker.workflowType, register.type)
    assertEquals(Launched(id, "start"), launch)
  }

  @Test fun `completion event order for workflow when finishes`() {
    val launcher = object : Launcher<Unit, Unit, String> {
      override fun launch(
        initialState: Unit,
        workflows: WorkflowPool
      ): Workflow<Unit, Unit, String> = workflow { _, _ -> "done" }
    }
    val handle = WorkflowPool.handle(launcher::class)
    val id = handle.id
    pool.register(launcher)

    pool.workflowUpdate(handle)

    val (remove, finish, launch) = monitor.events.asReversed()
    launch as Launched
    finish as Finished
    remove as RemovedFromPool
    assertEquals(Finished(id, "done"), finish)
    assertEquals(RemovedFromPool(id), remove)
  }

  @Test fun `completion event order for worker when finishes`() {
    val worker = worker<String, String> { it }
    val id = worker.workflowType.makeWorkflowId()

    pool.workerResult(worker, "foo")

    val (remove, finish, launch) = monitor.events.asReversed()
    launch as Launched
    finish as Finished
    remove as RemovedFromPool
    assertEquals(Finished(id, "foo"), finish)
    assertEquals(RemovedFromPool(id), remove)
  }

  @Test fun `completion event order for workflow when abandoned`() {
    val launcher = object : Launcher<Unit, Unit, String> {
      override fun launch(
        initialState: Unit,
        workflows: WorkflowPool
      ): Workflow<Unit, Unit, String> = workflow { _, _ -> suspendCoroutine {} }
    }
    val handle = WorkflowPool.handle(launcher::class)
    val id = handle.id
    pool.register(launcher)

    pool.workflowUpdate(handle)
    pool.abandonWorkflow(handle)

    monitor.assertEventsEndsWith(Abandoned(id))

    // TODO(https://github.com/square/workflow/issues/131) Abandoned workflows
    // should be removed from the pool.
    // monitor.assertEventsEndsWith(RemovedFromPool(id))
  }

  @Test fun `completion event order for worker when abandoned`() {
    val worker = worker<String, String> { suspendCoroutine { } }
    val id = worker.workflowType.makeWorkflowId()

    pool.workerResult(worker, "foo")
    pool.abandonWorker(worker)

    monitor.assertEventsEndsWith(Abandoned(id))

    // TODO(https://github.com/square/workflow/issues/131) Abandoned workflows
    // should be removed from the pool.
    // monitor.assertEventsEndsWith(RemovedFromPool(id))
  }

  @Test fun `emits StateChanged when workflow emits new state`() {
    val states = Channel<String>(UNLIMITED)
    val launcher = object : Launcher<String, String, Unit> {
      override fun launch(
        initialState: String,
        workflows: WorkflowPool
      ): Workflow<String, String, Unit> = workflow { state, _ ->
        states.toChannel(state)
      }
    }
    pool.register(launcher)
    val handle = WorkflowPool.handle(launcher::class, "start")
    val id = handle.id

    states.offer("foo")
    pool.workflowUpdate(handle)
    monitor.assertEventsEndsWith(StateChanged(id, "foo"))

    states.offer("bar")
    pool.workflowUpdate(handle)
    states.offer("baz")
    pool.workflowUpdate(handle)
    monitor.assertEventsEndsWith(
        StateChanged(id, "foo"),
        UpdateRequested(id, "start"),
        StateChanged(id, "bar"),
        UpdateRequested(id, "start"),
        StateChanged(id, "baz")
    )
  }

  @Test fun `emit ReceivedEvent when event is sent to running workflow`() {
    val launcher = object : Launcher<Unit, String, Unit> {
      override fun launch(
        initialState: Unit,
        workflows: WorkflowPool
      ): Workflow<Unit, String, Unit> = workflow { _, _ ->
        suspendCoroutine<Nothing> { }
      }
    }
    val handle = WorkflowPool.handle(launcher::class)
    val id = handle.id
    pool.register(launcher)
    pool.workflowUpdate(handle)

    pool.input(handle)
        .sendEvent("foo")
    monitor.assertEventsEndsWith(ReceivedEvent(id, "foo"))

    pool.input(handle)
        .sendEvent("bar")
    monitor.assertEventsEndsWith(ReceivedEvent(id, "bar"))
  }

  @Test fun `doesn't emit ReceivedEvent when event is sent to unstarted workflow`() {
    val launcher = object : Launcher<Unit, String, Unit> {
      override fun launch(
        initialState: Unit,
        workflows: WorkflowPool
      ): Workflow<Unit, String, Unit> = workflow { _, _ ->
        suspendCoroutine<Nothing> { }
      }
    }
    val handle = WorkflowPool.handle(launcher::class)
    pool.register(launcher)

    pool.input(handle)
        .sendEvent("foo")
    assertFalse(monitor.events.any { it is ReceivedEvent })
  }

  @Test fun `doesn't emit ReceivedEvent when event is sent to completed workflow`() {
    val launcher = object : Launcher<Unit, String, Unit> {
      override fun launch(
        initialState: Unit,
        workflows: WorkflowPool
      ): Workflow<Unit, String, Unit> = workflow { _, _ -> }
    }
    val handle = WorkflowPool.handle(launcher::class)
    pool.register(launcher)
    pool.workflowUpdate(handle)

    pool.input(handle)
        .sendEvent("foo")
    assertFalse(monitor.events.any { it is ReceivedEvent })
  }

  private class TestWorkflowPoolMonitor : WorkflowPoolMonitor {

    data class RecordedEvent(
      val pool: WorkflowPool,
      val event: WorkflowPoolMonitorEvent
    )

    val rawEvents = mutableListOf<RecordedEvent>()

    val events get() = rawEvents.map { it.event }

    fun assertEvents(vararg events: WorkflowPoolMonitorEvent) =
      assertEquals(events.asList(), this.events)

    fun assertEventsEndsWith(vararg events: WorkflowPoolMonitorEvent) =
      assertEquals(events.asList(), this.events.takeLast(events.size))

    override fun report(
      pool: WorkflowPool,
      event: WorkflowPoolMonitorEvent
    ) {
      rawEvents += RecordedEvent(pool, event)
    }
  }
}
