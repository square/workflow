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

import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.FinishedWorkflow
import com.squareup.workflow.Reaction
import com.squareup.workflow.RunWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHandle
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.makeWorkflowId
import com.squareup.workflow.register
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.Background
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.Cancel
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.Pause
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.Resume
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.RunEchoJob
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterEvent.RunImmediateJob
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterLauncher
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterState.Idle
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterState.Paused
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterState.RunningEchoJob
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.OuterState.RunningImmediateJob
import com.squareup.workflow.rx2.Rx2NestedWorkflowsIntegrationTest.StringEchoer
import io.reactivex.Single
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Tests [discreteStateWorkflow], [WorkflowHandle] and [WorkflowPool], with an [OuterLauncher]
 * that can run, pause, background, resume named instances of an [StringEchoer].
 *
 * This IS NOT meant to be an example of how to write workflows in production.
 * The background workflow thing is very fragile, it's just an amusing way
 * to exercise running concurrent workflows of the same time.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Rx2NestedWorkflowsIntegrationTest {
  @Rule @JvmField val assemblyTracking = RxAssemblyTrackingRule()

  @Test fun `run and see result after state update`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "fnord")
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("fnord"))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun `run and see result after no state updates`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun `pause does abandon and restart from saved state`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "able")

    workflow.sendEvent(Pause)
    assertThat(pool.peekWorkflowsCount).isZero()

    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("able"))
  }

  @Test fun `pause immediately and resume and complete immediately`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunEchoJob("job"))
    workflow.sendEvent(Pause)
    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun `cancel abandons`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "able")
    sendEchoEvent("job", "baker")

    workflow.sendEvent(Cancel)
    assertThat(pool.peekWorkflowsCount).isZero()

    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", STOP_ECHO_JOB)

    // We used the same job id, but the state changes from the previous session were dropped.
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun `background demonstrates concurrent workflows of the same type`() {
    val workflow = outerLauncher.launch()

    workflow.sendEvent(RunEchoJob("job1"))
    workflow.sendEvent(Background)

    workflow.sendEvent(RunEchoJob("job2"))
    workflow.sendEvent(Background)

    workflow.sendEvent(RunEchoJob("job3"))
    workflow.sendEvent(Background)

    sendEchoEvent("job1", "biz")
    sendEchoEvent("job2", "baz")
    sendEchoEvent("job3", "bang")

    workflow.sendEvent(RunEchoJob("job1"))
    sendEchoEvent("job1", STOP_ECHO_JOB)

    workflow.sendEvent(RunEchoJob("job2"))
    sendEchoEvent("job2", STOP_ECHO_JOB)

    workflow.sendEvent(RunEchoJob("job3"))
    sendEchoEvent("job3", STOP_ECHO_JOB)

    assertThat(results).isEqualTo(listOf("biz", "baz", "bang"))
  }

  @Test fun `sync single on success in nested workflow`() {
    val workflow = outerLauncher.launch()
    workflow.sendEvent(RunImmediateJob)

    workflow.sendEvent(RunEchoJob("fnord"))
    sendEchoEvent("fnord", STOP_ECHO_JOB)

    assertThat(results).isEqualTo(listOf("Finished ${ImmediateOnSuccess::class}", NEW_ECHO_JOB))

    assertThat(pool.peekWorkflowsCount).isZero()
  }

  companion object {
    const val NEW_ECHO_JOB = "*NEW ECHO JOB*"
    const val STOP_ECHO_JOB = "*STOP ECHO JOB*"
  }

  /**
   * Basically a job that can emit strings at arbitrary times, and whose
   * result code is the last string it emitted.
   */
  private class StringEchoer : Launcher<String, String, String> {
    override fun launch(
      initialState: String,
      workflows: WorkflowPool
    ): Workflow<String, String, String> =
      workflows.discreteStateWorkflow(initialState, "StringEchoer") { state, events, _ ->
        events.select {
          onEvent<String> { event ->
            when (event) {
              STOP_ECHO_JOB -> FinishWith(state)
              else -> EnterState(event)
            }
          }
        }
      }
  }

  private val echoLauncher = StringEchoer()

  private class ImmediateOnSuccess : Launcher<Unit, String, Unit> {
    override fun launch(
      initialState: Unit,
      workflows: WorkflowPool
    ): Workflow<Unit, String, Unit> = workflows.discreteStateWorkflow(
        initialState, "ImmediateOnSuccess"
    ) { _, events, _ ->
      events.select {
        workflows.onWorkerResult(singleWorker<Unit, String> { Single.just("fnord") }, Unit) {
          FinishWith(Unit)
        }
      }
    }
  }

  private val immediateLauncher = ImmediateOnSuccess()

  private sealed class OuterState {
    object Idle : OuterState()

    data class RunningEchoJob(
      val echoer: RunWorkflow<String, String, String>
    ) : OuterState()

    data class Paused(
      val echoer: RunWorkflow<String, String, String>
    ) : OuterState()

    object RunningImmediateJob : OuterState() {
      val job = RunWorkflow(ImmediateOnSuccess::class.makeWorkflowId(), Unit)
    }
  }

  private sealed class OuterEvent {
    data class RunEchoJob(
      val echoer: RunWorkflow<String, String, String>
    ) : OuterEvent() {
      constructor(
        id: String,
        state: String = NEW_ECHO_JOB
      ) : this(RunWorkflow(StringEchoer::class.makeWorkflowId(id), state))
    }

    object RunImmediateJob : OuterEvent()

    /** Ctrl-C */
    object Cancel : OuterEvent()

    /** Ctrl-S */
    object Pause : OuterEvent()

    /** Ctrl-Q */
    object Resume : OuterEvent()

    /** Ctrl-Z */
    object Background : OuterEvent()
  }

  private val results = mutableListOf<String>()

  private val pool = WorkflowPool()

  /**
   * One running job, which might be paused or backgrounded. Any number of background jobs.
   *
   * There are two kinds of jobs:
   *
   *  - [StringEchoer], whose state is the last String event it received
   *  - [ImmediateOnSuccess], which immediately completes
   */
  private inner class OuterLauncher : Launcher<OuterState, OuterEvent, Unit> {
    fun launch() = launch(Idle, pool)

    override fun launch(
      initialState: OuterState,
      workflows: WorkflowPool
    ): Workflow<OuterState, OuterEvent, Unit> {
      workflows.register(echoLauncher)
      workflows.register(immediateLauncher)
      val workflow = workflows.discreteStateWorkflow(initialState, "OuterLauncher", ::onReact)
      workflow.toCompletable()
          .subscribe { workflows.abandonAll() }
      return workflow
    }

    private fun onReact(
      state: OuterState,
      events: EventChannel<OuterEvent>,
      workflows: WorkflowPool
    ): Single<out Reaction<OuterState, Unit>> = when (state) {

      Idle -> events.select {
        onEvent<RunEchoJob> { EnterState(RunningEchoJob(it.echoer)) }
        onEvent<RunImmediateJob> { EnterState(RunningImmediateJob) }
        onEvent<Cancel> { FinishWith(Unit) }
      }

      is RunningEchoJob -> events.select {
        workflows.onWorkflowUpdate(state.echoer) {
          when (it) {
            is RunWorkflow -> EnterState(RunningEchoJob(it))
            is FinishedWorkflow -> {
              results += it.result
              EnterState(Idle)
            }
          }
        }

        onEvent<Pause> {
          workflows.abandonWorkflow(state.echoer)
          EnterState(Paused(state.echoer))
        }

        onEvent<Background> { EnterState(Idle) }

        onEvent<Cancel> {
          workflows.abandonWorkflow(state.echoer)
          EnterState(Idle)
        }
      }

      is Paused -> events.select {
        onEvent<Resume> { EnterState(RunningEchoJob(state.echoer)) }
        onEvent<Cancel> { EnterState(Idle) }
      }

      is RunningImmediateJob -> events.select {
        workflows.onWorkflowUpdate(state.job) {
          when (it) {
            is RunWorkflow -> throw AssertionError(
                "Should never re-enter $RunningImmediateJob."
            )
            is FinishedWorkflow -> {
              results += "Finished ${ImmediateOnSuccess::class}"
              EnterState(Idle)
            }
          }
        }
      }
    }
  }

  private val outerLauncher = OuterLauncher()

  private fun sendEchoEvent(
    echoJobId: String,
    event: String
  ) {
    pool.input(StringEchoer::class.makeWorkflowId(echoJobId))
        .sendEvent(event)
  }
}
