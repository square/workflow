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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.legacy.EnterState
import com.squareup.workflow.legacy.FinishWith
import com.squareup.workflow.legacy.Finished
import com.squareup.workflow.legacy.Reaction
import com.squareup.workflow.legacy.Running
import com.squareup.workflow.legacy.Workflow
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.WorkflowPool.Handle
import com.squareup.workflow.legacy.WorkflowUpdate
import com.squareup.workflow.legacy.register
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
class Rx2WorkflowPoolIntegrationTest {
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

  val pool = WorkflowPool().apply { register(myReactor) }

  private fun handle(
    state: String = "",
    name: String = ""
  ) = WorkflowPool.handle(myReactor::class, state, name)

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
    assertThat(launchCount).isEqualTo(0)
  }

  @Test fun `waits for state after current`() {
    val handle = handle(NEW)
    val nestedStateSub = pool.testOnWorkflowUpdate(handle)
    nestedStateSub.assertNoValues()

    val input = pool.input(handle)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    input.sendEvent(NEW)
    nestedStateSub.assertNoValues()

    input.sendEvent("fnord")
    assertThat((nestedStateSub.values()[0] as Running).handle.state).isEqualTo("fnord")
    nestedStateSub.assertComplete()
  }

  @Test fun `reports result`() {
    val handle = handle(NEW)

    // We don't actually care about the reaction, just want the workflow
    // to start.
    pool.testOnWorkflowUpdate(handle)

    // Advance the state a bit.
    val input = pool.input(handle)
    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    // Check that we get the result we expect after the above events.
    val resultSub = pool.testOnWorkflowUpdate(handle("charlie"))
    resultSub.assertNoValues()

    input.sendEvent(STOP)
    resultSub.assertValue(Finished("charlie"))
    resultSub.assertComplete()
  }

  @Test fun `reports immediate result`() {
    val handle = handle(NEW)
    val resultSub = pool.testOnWorkflowUpdate(handle)
    resultSub.assertNoValues()

    val input = pool.input(handle)
    input.sendEvent(STOP)
    resultSub.assertValue(Finished(NEW))
    resultSub.assertComplete()
  }

  @Test fun `inits once per next state`() {
    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)

    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)

    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun `inits once per result`() {
    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)

    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)

    pool.testOnWorkflowUpdate(handle())
    assertThat(launchCount).isEqualTo(1)
  }

  @Test fun `routes events`() {
    val handle = handle()
    pool.testOnWorkflowUpdate(handle)
    val input = pool.input(handle)

    input.sendEvent("able")
    input.sendEvent("baker")
    input.sendEvent("charlie")

    assertThat(eventsSent).isEqualTo(listOf("able", "baker", "charlie"))
  }

  @Test fun `drops late events`() {
    val handle = handle()
    val input = pool.input(handle)
    pool.testOnWorkflowUpdate(handle)

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
    val handle = handle()
    val input = pool.input(handle)
    input.sendEvent("able")
    pool.testOnWorkflowUpdate(handle)
    input.sendEvent("baker")

    assertThat(eventsSent).isEqualTo(listOf("baker"))
  }

  @Test fun `resumes routing events`() {
    val handle = handle()
    pool.testOnWorkflowUpdate(handle)
    val input = pool.input(handle)

    input.sendEvent("able")
    input.sendEvent("baker")
    // End the workflow.
    input.sendEvent(STOP)
    // Consume the completed workflow.
    pool.testOnWorkflowUpdate(handle)

    input.sendEvent("charlie")
    input.sendEvent("delta")
    pool.testOnWorkflowUpdate(handle)

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
    assertThat(abandonCount).isEqualTo(0)
    pool.testOnWorkflowUpdate(handle(NEW))
    pool.abandonWorkflow(handle())
    pool.abandonWorkflow(handle())
    pool.abandonWorkflow(handle())
    assertThat(abandonCount).isEqualTo(1)
  }

  @Test fun `abandon emits nothing and does not complete`() {
    val alreadyInNewState = handle(NEW)
    val stateSub = pool.testOnWorkflowUpdate(alreadyInNewState)

    pool.abandonWorkflow(alreadyInNewState)

    stateSub.assertNoValues()
    stateSub.assertNotTerminated()
  }
}

/**
 * Helper for calling and subscribing to
 * [com.squareup.workflow.rx2.EventSelectBuilder.onWorkflowUpdate] for tests.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : Any, E : Any, O : Any> WorkflowPool.testOnWorkflowUpdate(
  handle: Handle<S, E, O>
): TestObserver<WorkflowUpdate<S, E, O>> = Channel<Nothing>().asEventChannel()
    .select<WorkflowUpdate<S, E, O>> {
      onWorkflowUpdate(handle) { it }
    }.test() as TestObserver<WorkflowUpdate<S, E, O>>
