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
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.Background
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.Cancel
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.Pause
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.Resume
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.RunEchoJob
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterEvent.RunImmediateJob
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterReactor
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterState.Idle
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterState.Paused
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterState.RunningEchoJob
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.OuterState.RunningImmediateJob
import com.squareup.workflow.legacy.rx2.Rx2ReactorIntegrationTest.StringEchoer
import io.reactivex.Single
import org.junit.Rule
import org.junit.Test

/**
 * Tests [Reactor], [WorkflowUpdate] and [WorkflowPool], with an [OuterReactor]
 * that can run, pause, background, resume named instances of an [StringEchoer].
 *
 * This IS NOT meant to be an example of how to write workflows in production.
 * The background workflow thing is very fragile, it's just an amusing way
 * to exercise running concurrent workflows of the same time.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Rx2ReactorIntegrationTest {
  @Rule @JvmField val assemblyTracking = RxAssemblyTrackingRule()

  @Test fun `run and see result after state update`() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "fnord")
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("fnord"))
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)
  }

  @Test fun `run and see result after no state updates`() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)
  }

  @Test fun `pause does abandon and restart from saved state`() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "able")

    workflow.sendEvent(Pause)
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)

    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("able"))
  }

  @Test fun `pause immediately and resume and complete immediately`() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    workflow.sendEvent(Pause)
    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)
  }

  @Test fun `cancel abandons`() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "able")
    sendEchoEvent("job", "baker")

    workflow.sendEvent(Cancel)
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)

    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", STOP_ECHO_JOB)

    // We used the same job id, but the state changes from the previous session were dropped.
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isEqualTo(0)
  }

  @Test fun `background demonstrates concurrent workflows of the same type`() {
    val workflow = outerReactor.launch()

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
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunImmediateJob)

    workflow.sendEvent(RunEchoJob("fnord"))
    sendEchoEvent("fnord", STOP_ECHO_JOB)

    assertThat(results).isEqualTo(listOf("Finished ${ImmediateOnSuccess::class}", NEW_ECHO_JOB))

    assertThat(pool.peekWorkflowsCount).isEqualTo(0)
  }

  companion object {
    const val NEW_ECHO_JOB = "*NEW ECHO JOB*"
    const val STOP_ECHO_JOB = "*STOP ECHO JOB*"
  }

  /**
   * Basically a job that can emit strings at arbitrary times, and whose
   * result code is the last string it emitted.
   */
  class StringEchoer : Reactor<String, String, String> {
    override fun onReact(
      state: String,
      events: EventChannel<String>,
      workflows: WorkflowPool
    ): Single<out Reaction<String, String>> = events.select {
      onEvent<String> { event ->
        when (event) {
          STOP_ECHO_JOB -> FinishWith(state)
          else -> EnterState(event)
        }
      }
    }

    override fun launch(
      initialState: String,
      workflows: WorkflowPool
    ): Workflow<String, String, String> = doLaunch(initialState, workflows)
  }

  val echoReactor = StringEchoer()

  class ImmediateOnSuccess : Reactor<Unit, String, Unit> {
    override fun onReact(
      state: Unit,
      events: EventChannel<String>,
      workflows: WorkflowPool
    ): Single<out Reaction<Unit, Unit>> = events.select {
      workflows.onWorkerResult(singleWorker<Unit, String> { Single.just("fnord") }, Unit) {
        FinishWith(Unit)
      }
    }

    override fun launch(
      initialState: Unit,
      workflows: WorkflowPool
    ): Workflow<Unit, String, Unit> = doLaunch(initialState, workflows)
  }

  val immediateReactor = ImmediateOnSuccess()

  sealed class OuterState {
    object Idle : OuterState()

    data class RunningEchoJob(
      val echoer: Handle<String, String, String>
    ) : OuterState()

    data class Paused(
      val echoer: Handle<String, String, String>
    ) : OuterState()

    object RunningImmediateJob : OuterState() {
      val job = WorkflowPool.handle(ImmediateOnSuccess::class)
    }
  }

  sealed class OuterEvent {
    data class RunEchoJob(
      val echoer: Handle<String, String, String>
    ) : OuterEvent() {
      constructor(
        id: String,
        state: String = NEW_ECHO_JOB
      ) : this(WorkflowPool.handle(StringEchoer::class, state, id))
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

  val results = mutableListOf<String>()

  val pool = WorkflowPool()

  /**
   * One running job, which might be paused or backgrounded. Any number of background jobs.
   *
   * There are two kinds of jobs:
   *
   *  - [StringEchoer], whose state is the last String event it received
   *  - [ImmediateOnSuccess], which immediately completes
   */
  inner class OuterReactor : Reactor<OuterState, OuterEvent, Unit> {
    override fun onReact(
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
            is Running -> EnterState(RunningEchoJob(it.handle))
            is Finished -> {
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
            is Running -> throw AssertionError(
                "Should never re-enter $RunningImmediateJob."
            )
            is Finished -> {
              results += "Finished ${ImmediateOnSuccess::class}"
              EnterState(Idle)
            }
          }
        }
      }
    }

    fun launch() = launch(Idle, pool)

    override fun launch(
      initialState: OuterState,
      workflows: WorkflowPool
    ): Workflow<OuterState, OuterEvent, Unit> {
      workflows.register(echoReactor)
      workflows.register(immediateReactor)
      val workflow = doLaunch(initialState, workflows)
      workflow.toCompletable()
          .subscribe { workflows.abandonAll() }
      return workflow
    }
  }

  val outerReactor = OuterReactor()

  fun sendEchoEvent(
    echoJobId: String,
    event: String
  ) {
    pool.input(WorkflowPool.handle(StringEchoer::class, "ignored", echoJobId))
        .sendEvent(event)
  }
}
