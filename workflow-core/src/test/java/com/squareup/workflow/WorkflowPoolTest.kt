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
package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Id
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.ReceiveChannel
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

  inner class DelegatingState(
    override val delegateState: String = "",
    name: String = ""
  ) : Delegating<String, String, String> {
    override val id: Id<String, String, String> = myReactor.workflowType.makeId(name)
  }

  @Test fun metaTest_myReactorReportsStatesAndResult() {
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

  @Test fun metaTest_myReactorAbandonsAndStateCompletes() {
    val workflow = myReactor.launch(NEW, pool)
    val abandoned = AtomicBoolean(false)

    workflow.invokeOnCompletion { abandoned.set(true) }

    assertFalse(abandoned.get())
    workflow.cancel()
    assertTrue(abandoned.get())
  }

  @Test fun noEagerLaunch() {
    assertEquals(0, launchCount)
  }

  @Test fun waitsForStateAfterCurrent() {
    val delegatingState = DelegatingState(NEW)
    val nestedStateSub = pool.nextDelegateReaction(delegatingState)
    assertFalse(nestedStateSub.isCompleted)

    val input = pool.input(delegatingState.id)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    assertFalse(nestedStateSub.isCompleted)

    input.sendEvent("fnord")
    assertEquals(EnterState("fnord"), nestedStateSub.getCompleted())
  }

  @Test fun reportsResult() {
    val firstState = DelegatingState(NEW)

    // We don't actually care about the reaction, just want the workflow to start.
    pool.nextDelegateReaction(firstState)

    // Advance the state a bit.
    val input = pool.input(firstState.id)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.nextDelegateReaction(DelegatingState("charlie"))
    assertFalse(resultSub.isCompleted)

    input.sendEvent(STOP)
    assertEquals(FinishWith("charlie"), resultSub.getCompleted())
  }

  @Test fun reportsImmediateResult() {
    val delegatingState = DelegatingState(NEW)
    val resultSub = pool.nextDelegateReaction(delegatingState)
    assertFalse(resultSub.isCompleted)

    val input = pool.input(delegatingState.id)
    input.sendEvent(STOP)
    assertEquals(FinishWith(NEW), resultSub.getCompleted())
  }

  @Test fun initsOncePerNextState() {
    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)

    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)

    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)
  }

  @Test fun initsOncePerResult() {
    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)

    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)

    pool.nextDelegateReaction(DelegatingState())
    assertEquals(1, launchCount)
  }

  @Test fun routesEvents() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.workflowType.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertEquals(listOf("able", "baker", "charlie"), eventsSent)
  }

  @Test fun dropsLateEvents() {
    val input = pool.input(myReactor.workflowType.makeId())
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    input.sendEvent("charlie")
    assertEquals(listOf("able", "baker", STOP), eventsSent)
  }

  @Test fun dropsEarlyEvents() {
    val input = pool.input(myReactor.workflowType.makeId())
    input.sendEvent("able")
    pool.nextDelegateReaction(DelegatingState())
    input.sendEvent("baker")

    assertEquals(listOf("baker"), eventsSent)
  }

  @Test fun resumesRoutingEvents() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.workflowType.makeId())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("echo")
    input.sendEvent("foxtrot")

    assertEquals(listOf("able", "baker", STOP, "echo", "foxtrot"), eventsSent)
  }

  @Test fun abandonsOnlyOnce() {
    assertEquals(0, abandonCount)
    pool.nextDelegateReaction(DelegatingState(NEW))
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    assertEquals(1, abandonCount)
  }

  @Test fun abandonCancelsDeferred() {
    val alreadyInNewState = DelegatingState(NEW)
    val id = alreadyInNewState.id

    val stateSub = pool.nextDelegateReaction(alreadyInNewState)

    pool.abandonDelegate(id)

    assertTrue(stateSub.isCompletedExceptionally)
    assertTrue(stateSub.getCompletionExceptionOrNull() is CancellationException)
  }
}
