/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DeferredResultUnused", "MemberVisibilityCanBePrivate")
class WorkflowPoolTest {
  companion object {
    const val NEW = "*NEW*"
    const val STOP = "*STOP*"
  }

  val eventsSent = mutableListOf<String>()

  var abandonCount = 0
  var launchCount = 0

  inner class MyReactor : Reactor<String, String, String> {
    override suspend fun onReact(
      state: String,
      events: ReceiveChannel<String>,
      workflows: WorkflowPool
    ): Reaction<String, String> = events.receive().let {
      eventsSent += it
      if (it == STOP) FinishWith(state) else EnterState(it)
    }

    override fun launch(
      initialState: String,
      workflows: WorkflowPool
    ): Workflow<String, String, String> {
      launchCount++
      val workflow = doLaunch(initialState, workflows)
      workflow.invokeOnCompletion {
        if (it is CancellationException) {
          abandonCount++
        }
      }
      return workflow
    }
  }

  val myReactor = MyReactor()

  val pool = WorkflowPool().apply { register(myReactor) }

  private fun handle(
    state: String = "",
    name: String = ""
  ) = WorkflowPool.handle(myReactor::class, state, name)

  @Test fun `meta test myReactor reports states and result`() {
    val workflow = myReactor.launch(NEW, pool)
    val stateSub = workflow.openSubscriptionToState()

    assertEquals(NEW, stateSub.poll())
    workflow.sendEvent("able")
    assertEquals("able", stateSub.poll())
    workflow.sendEvent("baker")
    assertEquals("baker", stateSub.poll())
    workflow.sendEvent("charlie")
    assertEquals("charlie", stateSub.poll())
    workflow.sendEvent(STOP)
    assertTrue(stateSub.isClosedForReceive)

    assertEquals("charlie", workflow.getCompleted())
  }

  @Test fun `meta test myReactor abandons and state completes`() {
    val workflow = myReactor.launch(NEW, pool)
    val abandoned = AtomicBoolean(false)

    workflow.invokeOnCompletion { abandoned.set(true) }

    assertFalse(abandoned.get())
    workflow.cancel()
    assertTrue(abandoned.get())
  }

  @Test fun `no eager launch`() {
    assertEquals(0, launchCount)
  }

  @Test fun `waits for state after current`() {
    val handle = handle(NEW)
    val nestedStateSub = pool.workflowUpdate(handle)
    assertFalse(nestedStateSub.isCompleted)

    val input = pool.input(handle)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    assertFalse(nestedStateSub.isCompleted)

    input.sendEvent("fnord")
    assertEquals(handle("fnord"), (nestedStateSub.getCompleted() as Running).handle)
  }

  @Test fun `reports result`() {
    val firstState = handle(NEW)

    // We don't actually care about the reaction, just want the workflow to start.
    pool.workflowUpdate(firstState)

    // Advance the state a bit.
    val input = pool.input(firstState)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.workflowUpdate(handle("charlie"))
    assertFalse(resultSub.isCompleted)

    input.sendEvent(STOP)
    assertEquals(Finished("charlie"), resultSub.getCompleted())
  }

  @Test fun `reports immediate result`() {
    val handle = handle(NEW)
    val resultSub = pool.workflowUpdate(handle)
    assertFalse(resultSub.isCompleted)

    val input = pool.input(handle)
    input.sendEvent(STOP)
    assertEquals(Finished(NEW), resultSub.getCompleted())
  }

  @Test fun `inits once per next state`() {
    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)

    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)

    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)
  }

  @Test fun `inits once per result`() {
    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)

    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)

    pool.workflowUpdate(handle())
    assertEquals(1, launchCount)
  }

  @Test fun `routes events`() {
    pool.workflowUpdate(handle())
    val input = pool.input(handle())

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertEquals(listOf("able", "baker", "charlie"), eventsSent)
  }

  @Test fun `drops late events`() {
    val input = pool.input(handle())
    pool.workflowUpdate(handle())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    input.sendEvent("charlie")
    assertEquals(listOf("able", "baker", STOP), eventsSent)
  }

  @Test fun `drops early events`() {
    val input = pool.input(handle())
    input.sendEvent("able")
    pool.workflowUpdate(handle())
    input.sendEvent("baker")

    assertEquals(listOf("baker"), eventsSent)
  }

  @Test fun `workflow isn't dropped until result reported`() {
    pool.workflowUpdate(handle())
    val input = pool.input(handle())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)

    pool.workflowUpdate(handle())

    assertEquals(listOf("able", "baker", STOP), eventsSent)
  }

  @Test fun `resumes routing events`() {
    pool.workflowUpdate(handle())
    val input = pool.input(handle())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    // Consume the completed workflow.
    pool.workflowUpdate(handle())

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.workflowUpdate(handle())

    input.sendEvent("echo")
    input.sendEvent("foxtrot")

    assertEquals(listOf("able", "baker", STOP, "echo", "foxtrot"), eventsSent)
  }

  @Test fun `abandons only once`() {
    assertEquals(0, abandonCount)
    pool.workflowUpdate(handle(NEW))
    pool.abandonWorkflow(handle().id)
    pool.abandonWorkflow(handle().id)
    pool.abandonWorkflow(handle().id)
    assertEquals(1, abandonCount)
  }

  @Test fun `abandon cancels deferred`() {
    val alreadyInNewState = handle(NEW)
    val id = alreadyInNewState.id

    val stateSub = pool.workflowUpdate(alreadyInNewState)

    pool.abandonWorkflow(id)

    assertTrue(stateSub.isCancelled && stateSub.isCompleted)
    assertTrue(stateSub.getCompletionExceptionOrNull() is CancellationException)
  }
}
