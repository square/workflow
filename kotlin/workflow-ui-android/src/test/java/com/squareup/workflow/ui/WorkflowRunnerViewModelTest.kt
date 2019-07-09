package com.squareup.workflow.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.RenderContext
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.asWorker
import com.squareup.workflow.onWorkerOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@UseExperimental(ExperimentalWorkflowUi::class, ExperimentalCoroutinesApi::class)
class WorkflowRunnerViewModelTest {
  @get:Rule
  val rule: TestRule = InstantTaskExecutorRule()

  private val scope = CoroutineScope(Unconfined)
  @Suppress("RemoveRedundantSpreadOperator")
  private val viewRegistry = ViewRegistry(*emptyArray<ViewBinding<*>>())

  @Test fun snapshotUpdatedOnHostEmission() {
    val snapshot1 = Snapshot.of("one")
    val snapshot2 = Snapshot.of("two")
    val snapshotsChannel = Channel<RenderingAndSnapshot<Unit>>(UNLIMITED)
    val snapshotsFlow = flow { snapshotsChannel.consumeEach { emit(it) } }

    val runner = WorkflowRunnerViewModel(viewRegistry, snapshotsFlow, MutableLiveData(), scope)

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

    val runner = WorkflowRunnerViewModel(viewRegistry, emptyFlow(), MutableLiveData(), scope)

    assertThat(cancelled).isFalse()

    runner.clearForTest()
    assertThat(cancelled).isTrue()
  }

  @Test fun magilla() {
    val outputs = ConflatedBroadcastChannel<String>()

    val w = object : StatelessWorkflow<Unit, String, Unit>() {
      override fun render(
        input: Unit,
        context: RenderContext<Nothing, String>
      ) {
        context.onWorkerOutput(outputs.asWorker()) { emitOutput(it) }
      }
    }

    val factory = WorkflowRunnerViewModel.Factory(
        w, viewRegistry, flowOf(Unit), null, TestCoroutineDispatcher()
    )
    val model = factory.create(WorkflowRunnerViewModel::class.java)

    runBlocking {
      model.output.observeForever {
        assertThat(it).isEqualTo("Snot")
      }

      // java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked. See http://g.co/androidstudio/not-mocked for details.
      outputs.send("Fnord")
    }
  }
}
