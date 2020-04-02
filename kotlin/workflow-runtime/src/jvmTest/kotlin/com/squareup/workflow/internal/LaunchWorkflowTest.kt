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

import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.launchWorkflowImpl
import com.squareup.workflow.stateless
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LaunchWorkflowTest {

  private val scope = CoroutineScope(Unconfined)
  private val workflow = Workflow.stateless<Unit, String, String> { fail() }
      .asStatefulWorkflow()

  @AfterTest fun tearDown() {
    scope.cancel()
  }

  @Test fun `renderings flow replays to new collectors`() {
    var rendered = false
    val loop = simpleLoop { onRendering, _ ->
      onRendering(RenderingAndSnapshot("foo", Snapshot.EMPTY))
      rendered = true
      hang()
    }

    val renderings = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { it.renderingsAndSnapshots }

    assertTrue(rendered)
    runBlocking {
      assertEquals("foo", renderings.first().rendering)
    }
  }

  @Test fun `outputs flow does not replay to new collectors`() {
    var rendered = false
    val loop = simpleLoop { _, onOutput ->
      onOutput("foo")
      rendered = true
      hang()
    }

    val outputs = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { it.outputs }

    assertTrue(rendered)
    runBlocking {
      val outputsChannel = outputs.produceIn(this)
      yield()
      assertNull(outputsChannel.poll())

      // Let the test finish.
      outputsChannel.cancel()
    }
  }

  @Test fun `renderings flow is multicasted`() {
    val loop = simpleLoop { onRendering, _ ->
      onRendering(RenderingAndSnapshot("foo", Snapshot.EMPTY))
      hang()
    }

    val renderings = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { it.renderingsAndSnapshots }

    runBlocking {
      assertEquals("foo", renderings.first().rendering)
      assertEquals("foo", renderings.first().rendering)
    }
  }

  @Test fun `outputs flow is multicasted`() {
    val loop = simpleLoop { _, onOutput ->
      onOutput("foo")
      hang()
    }

    val (outputs1, outputs2) = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      Pair(
          session.outputs.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    runBlocking {
      assertEquals("foo", outputs1.receive())
      assertEquals("foo", outputs2.receive())
    }
  }

  @Test fun `renderings flow has no backpressure`() {
    val loop = simpleLoop { onRendering, _ ->
      onRendering(RenderingAndSnapshot("one", Snapshot.EMPTY))
      onRendering(RenderingAndSnapshot("two", Snapshot.EMPTY))
      onRendering(RenderingAndSnapshot("three", Snapshot.EMPTY))
      onRendering(RenderingAndSnapshot("four", Snapshot.EMPTY))
      onRendering(RenderingAndSnapshot("five", Snapshot.EMPTY))
      onRendering(RenderingAndSnapshot("six", Snapshot.EMPTY))
      hang()
    }

    val renderings = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { it.renderingsAndSnapshots }

    runBlocking {
      assertEquals("six", renderings.first().rendering)
    }
  }

  @Test fun `outputs flow has no backpressure when not subscribed`() {
    val loop = simpleLoop { _, onOutput ->
      onOutput("one")
      onOutput("two")
      hang()
    }

    val outputs = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { it.outputs }

    runBlocking {
      val outputsChannel = outputs.produceIn(this)
      yield()
      assertNull(outputsChannel.poll())

      // Let the test finish.
      outputsChannel.cancel()
    }
  }

  @Test fun `outputs flow honors backpressure when subscribed`() {
    // Used to assert ordering.
    val counter = AtomicInteger(0)
    val loop = simpleLoop { _, onOutput ->
      assertEquals(0, counter.getAndIncrement())
      onOutput("one")
      onOutput("two")
      onOutput("three")
      assertEquals(2, counter.getAndIncrement())
      onOutput("four")
      assertEquals(4, counter.getAndIncrement())
      hang()
    }

    val outputsChannel = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      // Disable buffering for this subscription channel.
      // There will still be some buffering:
      //  - BroadcastChannel buffer has the minimum buffer size of 1.
      //  - Implicit buffer from the asFlow coroutine.
      //  - Implicit buffer from the coroutine created by produceIn.
      session.outputs.buffer(0)
          .produceIn(this)
          .also { assertNull(it.poll()) }
    }

    runBlocking {
      outputsChannel.consume {
        assertEquals(1, counter.getAndIncrement())
        assertEquals("one", outputsChannel.poll())
        assertEquals(3, counter.getAndIncrement())
        assertEquals("two", outputsChannel.poll())
        assertEquals(5, counter.getAndIncrement())
        assertEquals("three", outputsChannel.poll())
        assertEquals("four", outputsChannel.poll())
        assertEquals(6, counter.getAndIncrement())
      }
    }
  }

  @Test fun `flows complete immediately when base context is already cancelled on start`() {
    val scope = scope + Job().apply { cancel() }

    val (renderings, outputs) = launchWorkflowImpl(
        scope,
        HangingLoop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { Pair(it.renderingsAndSnapshots, it.outputs) }

    runBlocking {
      assertTrue(renderings.toList().isEmpty())
      assertTrue(outputs.toList().isEmpty())
    }
  }

  @Test fun `cancelling base context cancels runtime`() {
    val parentJob = Job()
    val scope = scope + parentJob
    var cancelled = false

    val (renderings, outputs) = launchWorkflowImpl(
        scope,
        HangingLoop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      coroutineContext[Job]!!.invokeOnCompletion {
        cancelled = true
      }
      Pair(
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertFalse(cancelled)
    assertTrue(parentJob.children.count() > 0)
    assertFalse(renderings.isClosedForReceive)
    assertFalse(outputs.isClosedForReceive)

    scope.cancel()
    assertTrue(parentJob.children.count() == 0)
    assertTrue(renderings.isClosedForReceive)
    assertTrue(outputs.isClosedForReceive)
  }

  @Test fun `cancelling internal scope cancels runtime`() {
    val parentJob = Job()
    val scope = scope + parentJob

    val (job, renderings, outputs) = launchWorkflowImpl(
        scope,
        HangingLoop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      Triple(
          coroutineContext[Job]!!,
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertTrue(parentJob.children.count() > 0)
    assertFalse(renderings.isClosedForReceive)
    assertFalse(outputs.isClosedForReceive)

    job.cancel()
    assertTrue(parentJob.children.count() == 0)
    assertTrue(renderings.isClosedForReceive)
    assertTrue(outputs.isClosedForReceive)
  }

  @Test fun `runtime cancelled when workflow throws cancellation`() {
    val loop = simpleLoop { _, _ ->
      throw CancellationException("Workflow cancelled itself.")
    }
    val (job, renderings, outputs) = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      Triple(
          coroutineContext[Job]!!,
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertTrue(job.isCancelled)
    assertTrue(renderings.isClosedForReceive)
    assertTrue(outputs.isClosedForReceive)
  }

  @Test fun `error from renderings collector fails runtime`() {
    val parentJob = Job()
    val scope = scope + parentJob
    val trigger = CompletableDeferred<Unit>()
    val loop = simpleLoop { onRendering, _ ->
      // Need to emit something so collector invokes lambda.
      onRendering(RenderingAndSnapshot(Unit, Snapshot.EMPTY))
      hang()
    }

    val (job, renderings, outputs) = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      launch {
        trigger.await()
        session.renderingsAndSnapshots.collect { throw ExpectedException() }
      }
      Triple(
          coroutineContext[Job]!!,
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertTrue(job.isActive)
    assertTrue(parentJob.isActive)
    assertFalse(renderings.isClosedForReceive)
    assertFalse(outputs.isClosedForReceive)

    trigger.complete(Unit)
    assertTrue(job.isCancelled)
    assertTrue(parentJob.isCancelled)
    runBlocking {
      assertFails { renderings.consumeEach { } }
      assertFails { outputs.consumeEach { } }
    }
  }

  @Test fun `error from outputs collector fails runtime`() {
    val parentJob = Job()
    val scope = scope + parentJob
    val trigger = CompletableDeferred<Unit>()
    val loop = simpleLoop { _, onOutput ->
      // Keep emitting so it doesn't matter when outputs is collected.
      while (true) {
        onOutput(Unit)
        yield()
      }
      @Suppress("UNREACHABLE_CODE")
      fail("type inference bug")
    }

    val (job, renderings, outputs) = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      launch {
        trigger.await()
        session.outputs.collect { throw ExpectedException() }
      }
      Triple(
          coroutineContext[Job]!!,
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertTrue(job.isActive)
    assertTrue(parentJob.isActive)
    assertFalse(renderings.isClosedForReceive)
    assertFalse(outputs.isClosedForReceive)

    trigger.complete(Unit)
    runBlocking {
      // Outputs must come first.
      assertFails { outputs.consumeEach { } }
      assertFails { renderings.consumeEach { } }
    }
    assertTrue(job.isCancelled)
    assertTrue(parentJob.isCancelled)
  }

  @Test fun `error from beforeStart propagates up but doesn't fail parent job`() {
    val parentJob = Job()
    val scope = scope + parentJob

    assertFails {
      launchWorkflowImpl(
          scope,
          HangingLoop,
          workflow,
          emptyFlow(),
          initialSnapshot = null,
          initialState = null
      ) { throw ExpectedException() }
    }

    assertTrue(parentJob.isActive)
  }

  @Test fun `error from loop fails runtime`() {
    val parentJob = Job()
    val scope = scope + parentJob
    val loop = simpleLoop { _, _ ->
      throw ExpectedException()
    }

    val (job, renderings, outputs) = launchWorkflowImpl(
        scope,
        loop,
        workflow,
        emptyFlow(),
        initialSnapshot = null,
        initialState = null
    ) { session ->
      Triple(
          coroutineContext[Job]!!,
          session.renderingsAndSnapshots.produceIn(this),
          session.outputs.produceIn(this)
      )
    }

    assertTrue(job.isCancelled)
    assertTrue(parentJob.isCancelled)
    runBlocking {
      assertFails { renderings.consumeEach { } }
      assertFails { outputs.consumeEach { } }
    }
  }

  @Test fun `emits diagnostic events`() {
    val loop = simpleLoop { _, _ -> hang() }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { }
    val listener = RecordingDiagnosticListener()

    runBlocking {
      launchWorkflowImpl(
          scope = this,
          workflowLoop = loop,
          workflow = workflow.asStatefulWorkflow(),
          props = emptyFlow(),
          initialSnapshot = null,
          initialState = null,
          beforeStart = { it.diagnosticListener = listener }
      )
      yield()
      assertEquals(listOf("onRuntimeStarted"), listener.consumeEventNames())

      // Cancel the runtime so we can finish.
      coroutineContext.cancelChildren()
      yield()
      assertEquals(listOf("onRuntimeStopped"), listener.consumeEventNames())
    }
  }

  @Test fun `throws when workerContext contains Job`() {
    val loop = simpleLoop { _, _ -> hang() }
    val workflow = Workflow.stateless<Unit, Nothing, Unit> { }
    val workerContext = Job()

    val error = assertFailsWith<IllegalArgumentException> {
      runBlocking {
        launchWorkflowImpl(
            scope = this,
            workflowLoop = loop,
            workflow = workflow.asStatefulWorkflow(),
            props = emptyFlow(),
            initialSnapshot = null,
            initialState = null,
            beforeStart = {},
            workerContext = workerContext
        )
      }
    }
    assertEquals("Expected workerContext not to have a Job.", error.message)
  }
}

private suspend fun hang(): Nothing = suspendCancellableCoroutine { }

@Suppress("UNCHECKED_CAST")
private fun simpleLoop(
  block: suspend (
    onRendering: suspend (RenderingAndSnapshot<Any?>) -> Unit,
    onOutput: suspend (Any?) -> Unit
  ) -> Nothing
): WorkflowLoop = object : WorkflowLoop {
  override suspend fun <PropsT, StateT, OutputT : Any, RenderingT> runWorkflowLoop(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    props: Flow<PropsT>,
    initialSnapshot: Snapshot?,
    initialState: StateT?,
    workerContext: CoroutineContext,
    onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
    onOutput: suspend (OutputT) -> Unit,
    diagnosticListener: WorkflowDiagnosticListener?
  ): Nothing {
    block(
        onRendering as suspend (RenderingAndSnapshot<Any?>) -> Unit,
        onOutput as suspend (Any?) -> Unit
    )
  }
}

private object HangingLoop : WorkflowLoop {
  override suspend fun <PropsT, StateT, OutputT : Any, RenderingT> runWorkflowLoop(
    workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
    props: Flow<PropsT>,
    initialSnapshot: Snapshot?,
    initialState: StateT?,
    workerContext: CoroutineContext,
    onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
    onOutput: suspend (OutputT) -> Unit,
    diagnosticListener: WorkflowDiagnosticListener?
  ): Nothing = hang()
}

private class ExpectedException : RuntimeException()
