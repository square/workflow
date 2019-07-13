package com.squareup.workflow.ui

import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Test

@UseExperimental(ExperimentalWorkflowUi::class, ExperimentalCoroutinesApi::class)
class WorkflowRunnerViewModelTest {

  private val scope = CoroutineScope(Unconfined)
  @Suppress("RemoveRedundantSpreadOperator")
  private val viewRegistry = ViewRegistry(*emptyArray<ViewBinding<*>>())

  @Test fun snapshotUpdatedOnHostEmission() {
    val snapshot1 = Snapshot.of("one")
    val snapshot2 = Snapshot.of("two")
    val snapshotsChannel = Channel<RenderingAndSnapshot<Unit>>(UNLIMITED)
    val snapshotsFlow = flow { snapshotsChannel.consumeEach { emit(it) } }

    val runner = WorkflowRunnerViewModel(emptyFlow(), viewRegistry, snapshotsFlow, scope)

    assertThat(runner.getLastSnapshotForTest()).isEqualTo(Snapshot.EMPTY)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot1))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot1)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot2))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot2)
  }

  @Test fun hostCancelledOnCleared() {
    var cancelled = false
    scope.coroutineContext[Job]!!.invokeOnCompletion { e ->
      if (e is CancellationException) cancelled = true
    }
    val runner = WorkflowRunnerViewModel(emptyFlow(), viewRegistry, emptyFlow(), scope)

    assertThat(cancelled).isFalse()
    runner.result.test()
    assertThat(cancelled).isFalse()

    runner.clearForTest()
    assertThat(cancelled).isTrue()
  }
}
