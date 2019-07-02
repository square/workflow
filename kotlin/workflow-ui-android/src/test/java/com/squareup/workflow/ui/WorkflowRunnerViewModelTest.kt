package com.squareup.workflow.ui

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.Snapshot
import com.squareup.workflow.WorkflowHost
import com.squareup.workflow.WorkflowHost.RenderingAndSnapshot
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Test

@UseExperimental(ExperimentalWorkflowUi::class, ExperimentalCoroutinesApi::class)
class WorkflowRunnerViewModelTest {

  @Suppress("RemoveRedundantSpreadOperator")
  private val viewRegistry = ViewRegistry(*emptyArray<ViewBinding<*>>())

  @Test fun hostStartedLazilyOnRenderingsSubscription() {
    var started = false
    val host = object : WorkflowHost<Nothing, Unit> {
      override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<Unit>> = emptyFlow()
      override val outputs: Flow<Nothing> = emptyFlow()

      override fun start(): Job {
        started = true
        return Job()
      }
    }
    val runner = WorkflowRunnerViewModel(viewRegistry, host, Unconfined)

    assertThat(started).isFalse()

    runner.renderings.test()
    assertThat(started).isTrue()
  }

  @Test fun hostStartedLazilyOnOutputsSubscription() {
    var started = false
    val host = object : WorkflowHost<Unit, Unit> {
      override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<Unit>> = emptyFlow()
      override val outputs: Flow<Nothing> = emptyFlow()

      override fun start(): Job {
        started = true
        return Job()
      }
    }
    val runner = WorkflowRunnerViewModel(viewRegistry, host, Unconfined)

    assertThat(started).isFalse()

    runner.output.test()
    assertThat(started).isTrue()
  }

  @Test fun snapshotUpdatedOnHostEmission() {
    val snapshot1 = Snapshot.of("one")
    val snapshot2 = Snapshot.of("two")
    val snapshotsChannel = Channel<RenderingAndSnapshot<Unit>>(UNLIMITED)

    val host = object : WorkflowHost<Unit, Unit> {
      override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<Unit>> = flow {
        snapshotsChannel.consumeEach {
          emit(it)
        }
      }
      override val outputs: Flow<Nothing> = emptyFlow()

      override fun start() = Job()
    }
    val runner = WorkflowRunnerViewModel(viewRegistry, host, Unconfined)

    assertThat(runner.getLastSnapshotForTest()).isEqualTo(Snapshot.EMPTY)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot1))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot1)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot2))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot2)
  }

  @Test fun hostCancelledOnCleared() {
    var cancelled = false
    val host = object : WorkflowHost<Unit, Unit> {
      override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<Unit>> = emptyFlow()
      override val outputs: Flow<Nothing> = emptyFlow()

      private val job = Job().apply { invokeOnCompletion { cancelled = true } }

      override fun start(): Job = job
    }
    val runner = WorkflowRunnerViewModel(viewRegistry, host, Unconfined)

    assertThat(cancelled).isFalse()
    runner.output.test()
    assertThat(cancelled).isFalse()

    runner.clearForTest()
    assertThat(cancelled).isTrue()
  }
}
