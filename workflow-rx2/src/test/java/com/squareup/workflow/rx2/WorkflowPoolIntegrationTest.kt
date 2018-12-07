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
package com.squareup.workflow.rx2

import com.squareup.workflow.Delegating
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.register
import com.squareup.workflow.workflowType
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.experimental.CancellationException
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
class WorkflowPoolIntegrationTest {
  companion object {
    const val NEW = "*NEW*"
    const val STOP = "*STOP*"
  }

  val eventsSent = mutableListOf<String>()

  var abandonCount = 0
  var launchCount = 0

  inner class MyReactor : Reactor<String, String, String> {
    override fun onReact(
      state: String,
      events: EventChannel<String>,
      workflows: WorkflowPool
    ): Single<out Reaction<String, String>> = events.select {
      onEvent<String> {
        eventsSent += it
        if (it == STOP) FinishWith(state) else EnterState(it)
      }
    }

    override fun launch(
      initialState: String,
      workflows: WorkflowPool
    ): Workflow<String, String, String> {
      launchCount++
      val workflow = doLaunch(initialState, workflows)
      workflow.invokeOnCompletion { cause ->
        if (cause is CancellationException) {
          abandonCount++
        }
      }
      return workflow
    }
  }

  val myReactor = MyReactor()

  val pool = WorkflowPool()
      .apply { register(myReactor) }

  inner class DelegatingState(
    override val delegateState: String = "",
    name: String = ""
  ) : Delegating<String, String, String> {
    override val id: Id<String, String, String> = myReactor.workflowType.makeWorkflowId(name)
  }

  @Test fun `meta test myReactor reports states and result`() {
    val workflow = myReactor.launch(NEW, pool)
    val stateSub = workflow.state.test() as TestObserver<String>
    val resultSub = workflow.result.test() as TestObserver<String>

    workflow.sendEvent("able")
    workflow.sendEvent("baker")
    workflow.sendEvent("charlie")
    workflow.sendEvent(STOP)

    with(stateSub) {
      assertValues(NEW, "able", "baker", "charlie")
      assertTerminated()
    }
    with(resultSub) {
      assertValue("charlie")
      assertTerminated()
    }
  }

  @Test fun `meta test myReactor abandons and state completes`() {
    val workflow = myReactor.launch(NEW, pool)
    val abandoned = AtomicBoolean(false)

    workflow.toCompletable()
        .subscribe { abandoned.set(true) }
    assertThat(abandoned.get()).isFalse()
    workflow.cancel()
    assertThat(abandoned.get()).isTrue()
  }

  @Test fun `no eager launch`() {
    assertThat(launchCount).isZero()
  }

  @Test fun `waits for state after current`() {
    val delegatingState = DelegatingState(NEW)
    val nestedStateSub = pool.nextDelegateReaction(delegatingState)
        .test()
    nestedStateSub.assertNoValues()

    val input = pool.input(delegatingState.id)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    nestedStateSub.assertNoValues()

    input.sendEvent("fnord")
    nestedStateSub.assertValue(EnterState("fnord"))
    nestedStateSub.assertComplete()
  }

  @Test fun `reports result`() {
    val firstState = DelegatingState(NEW)

    // We don't actually care about the reaction, just want the workflow
    // to start.
    pool.nextDelegateReaction(firstState)

    // Advance the state a bit.
    val input = pool.input(firstState.id)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.nextDelegateReaction(DelegatingState("charlie"))
        .test()
    resultSub.assertNoValues()

    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith("charlie"))
    resultSub.assertComplete()
  }

  @Test fun `reports immediate result`() {
    val delegatingState = DelegatingState(NEW)
    val resultSub = pool.nextDelegateReaction(delegatingState)
        .test()
    resultSub.assertNoValues()

    val input = pool.input(delegatingState.id)
    input.sendEvent(STOP)
    resultSub.assertValue(FinishWith(NEW))
    resultSub.assertComplete()
  }

  @Test fun `inits once per next state`() {
    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun `inits once per result`() {
    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)

    pool.nextDelegateReaction(DelegatingState())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun `routes events`() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.workflowType.makeWorkflowId())

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertThat(eventsSent).isEqualTo(listOf("able", "baker", "charlie"))
  }

  @Test fun `drops late events`() {
    val input = pool.input(myReactor.workflowType.makeWorkflowId())
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    input.sendEvent("charlie")
    assertThat(eventsSent).isEqualTo(
        listOf(
            "able", "baker",
            STOP
        )
    )
  }

  @Test fun `drops early events`() {
    val input = pool.input(myReactor.workflowType.makeWorkflowId())
    input.sendEvent("able")
    pool.nextDelegateReaction(DelegatingState())
    input.sendEvent("baker")

    assertThat(eventsSent).isEqualTo(listOf("baker"))
  }

  @Test fun `resumes routing events`() {
    pool.nextDelegateReaction(DelegatingState())
    val input = pool.input(myReactor.workflowType.makeWorkflowId())

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    // Consume the completed workflow.
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.nextDelegateReaction(DelegatingState())

    input.sendEvent("echo")
    input.sendEvent("foxtrot")

    assertThat(eventsSent).isEqualTo(
        listOf(
            "able", "baker",
            STOP, "echo", "foxtrot"
        )
    )
  }

  @Test fun `abandons only once`() {
    assertThat(abandonCount).isZero()
    pool.nextDelegateReaction(DelegatingState(NEW))
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    pool.abandonDelegate(DelegatingState().id)
    assertThat(abandonCount).isEqualTo(1)
  }

  @Test fun `abandon emits nothing and does not complete`() {
    val alreadyInNewState = DelegatingState(NEW)
    val id = alreadyInNewState.id

    val stateSub = pool.nextDelegateReaction(alreadyInNewState)
        .test()

    pool.abandonDelegate(id)

    stateSub.assertNoValues()
    stateSub.assertNotTerminated()
  }
}
