package com.squareup.workflow.rx2

import com.squareup.workflow.Delegating
import com.squareup.workflow.EnterState
import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.makeId
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.Background
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.Cancel
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.Pause
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.Resume
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.RunEchoJob
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterEvent.RunImmediateJob
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterReactor
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterState.Idle
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterState.Paused
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterState.RunningEchoJob
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.OuterState.RunningImmediateJob
import com.squareup.workflow.rx2.ComposedReactorIntegrationTest.StringEchoer
import io.reactivex.Single
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Tests [Reactor], [Delegating] and [WorkflowPool], with an [OuterReactor]
 * that can run, pause, background, resume named instances of an [StringEchoer].
 *
 * This IS NOT meant to be an example of how to write workflows in production.
 * The background workflow thing is very fragile, it's just an amusing way
 * to exercise running concurrent workflows of the same time.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ComposedReactorIntegrationTest {
  @Rule @JvmField val assemblyTracking = RxAssemblyTrackingRule()

  @Test fun runAndSeeResultAfterStateUpdate() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "fnord")
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("fnord"))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun runAndSeeResultAfterNoStateUpdates() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun pause_doesAbandonAndRestartFromSavedState() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    sendEchoEvent("job", "able")

    workflow.sendEvent(Pause)
    assertThat(pool.peekWorkflowsCount).isZero()

    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf("able"))
  }

  @Test fun pauseImmediatelyAndResumeAndCompleteImmediately() {
    val workflow = outerReactor.launch()
    workflow.sendEvent(RunEchoJob("job"))
    workflow.sendEvent(Pause)
    workflow.sendEvent(Resume)

    sendEchoEvent("job", STOP_ECHO_JOB)
    assertThat(results).isEqualTo(listOf(NEW_ECHO_JOB))
    assertThat(pool.peekWorkflowsCount).isZero()
  }

  @Test fun cancel_abandons() {
    val workflow = outerReactor.launch()
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

  @Test fun background_demonstratesConcurrentWorkflowsOfTheSameType() {
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

  @Test fun syncSingleOnSuccessInNestedWorkflow() {
    val workflow = outerReactor.launch()
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
      onSuccess(Single.just("fnord")) {
        FinishWith(Unit)
      }
    }

    override fun launch(
      initialState: Unit,
      workflows: WorkflowPool
    ): Workflow<Unit, String, Unit> = doLaunch(initialState, workflows)
  }

  val immediateReactor =
    ImmediateOnSuccess()

  sealed class OuterState {
    object Idle : OuterState()

    data class RunningEchoJob constructor(
      override val id: Id<String, String, String>,
      override val delegateState: String
    ) : OuterState(), Delegating<String, String, String> {
      constructor(
        id: String,
        state: String
      ) : this(StringEchoer::class.makeId(id), state)
    }

    data class Paused(
      val id: String,
      val lastState: String
    ) : OuterState()

    object RunningImmediateJob : OuterState(), Delegating<Unit, String, Unit> {
      override val id = makeId()
      override val delegateState = Unit
    }
  }

  sealed class OuterEvent {
    data class RunEchoJob(
      val id: String,
      val state: String = NEW_ECHO_JOB
    ) : OuterEvent()

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
   */
  inner class OuterReactor : Reactor<OuterState, OuterEvent, Unit> {
    override fun onReact(
      state: OuterState,
      events: EventChannel<OuterEvent>,
      workflows: WorkflowPool
    ): Single<out Reaction<OuterState, Unit>> = when (state) {

      Idle -> events.select {
        onEvent<RunEchoJob> { EnterState(RunningEchoJob(it.id, it.state)) }
        onEvent<RunImmediateJob> { EnterState(RunningImmediateJob) }
        onEvent<Cancel> { FinishWith(Unit) }
      }

      is RunningEchoJob -> events.select {
        onSuccess(workflows.nextDelegateReaction(state)) {
          when (it) {
            is EnterState -> EnterState(state.copy(delegateState = it.state))
            is FinishWith -> {
              results += it.result
              EnterState(Idle)
            }
          }
        }

        onEvent<Pause> {
          workflows.abandonDelegate(state.id)
          EnterState(Paused(state.id.name, state.delegateState))
        }

        onEvent<Background> { EnterState(Idle) }

        onEvent<Cancel> {
          workflows.abandonDelegate(state.id)
          EnterState(Idle)
        }
      }

      is Paused -> events.select {
        onEvent<Resume> { EnterState(RunningEchoJob(state.id, state.lastState)) }
        onEvent<Cancel> { EnterState(Idle) }
      }

      is RunningImmediateJob -> workflows.nextDelegateReaction(state).map {
        when (it) {
          is EnterState -> throw AssertionError("Should never re-enter $RunningImmediateJob.")
          is FinishWith -> {
            results += "Finished ${ImmediateOnSuccess::class}"
            EnterState(Idle)
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
    pool.input(StringEchoer::class.makeId(echoJobId))
        .sendEvent(event)
  }
}
