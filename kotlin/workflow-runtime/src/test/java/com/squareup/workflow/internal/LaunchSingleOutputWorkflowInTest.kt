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

import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.asWorker
import com.squareup.workflow.launchSingleOutputWorkflowIn
import com.squareup.workflow.onWorkerOutput
import com.squareup.workflow.stateless
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LaunchSingleOutputWorkflowInTest {

  private class ExpectedException : RuntimeException()

  private val scope = CoroutineScope(Unconfined)
  private val subject = ConflatedBroadcastChannel<String>()
  private val workflow = Workflow.stateless<Unit, String, Unit> {
    onWorkerOutput(subject.asWorker()) { emitOutput(it) }
  }

  @AfterTest fun tearDown() {
    scope.cancel()
  }

  @Test fun `runtime is cancelled on output`() {
    var cancelled = false
    launchSingleOutputWorkflowIn(scope, workflow, flowOf(Unit)) { _, _ ->
      coroutineContext[Job]!!.invokeOnCompletion {
        if (it != null) cancelled = true
      }
    }

    assertFalse(cancelled)
    subject.offer("done")
    assertTrue(cancelled)
  }

  @Test fun `output gets delayed output`() {
    val output = launchSingleOutputWorkflowIn(scope, workflow, flowOf(Unit)) { _, output ->
      output
    }

    assertFalse(output.isCompleted)
    subject.offer("done")
    assertTrue(output.isCompleted)
    assertEquals("done", output.getCompleted())
  }

  @Test fun `output gets immediate output`() {
    // Trigger an output as soon as the workflow starts.
    subject.offer("done")
    val output = launchSingleOutputWorkflowIn(scope, workflow, flowOf(Unit)) { _, output ->
      output
    }

    assertTrue(output.isCompleted)
    runBlocking {
      yield()
    }
    assertEquals("done", output.getCompleted())
  }

  @Test fun `output gets cancelled`() {
    val output = launchSingleOutputWorkflowIn(scope, workflow, flowOf(Unit)) { _, output ->
      output
    }

    assertFalse(output.isCompleted)
    scope.cancel()
    assertTrue(output.isCancelled)
  }

  @Test fun `output gets error`() {
    val workflow = Workflow.stateless<Unit, Any, Unit> { throw ExpectedException() }
    val output = launchSingleOutputWorkflowIn(scope, workflow, flowOf(Unit)) { _, output ->
      output
    }

    assertTrue(output.isCancelled)
    val causeChain = generateSequence(output.getCompletionExceptionOrNull()) { it.cause }
    assertTrue(causeChain.last() is ExpectedException)
  }
}
