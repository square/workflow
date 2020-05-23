package com.squareup.workflow.ui

import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.asWorker
import com.squareup.workflow.runningWorker
import com.squareup.workflow.stateless
import com.squareup.workflow.ui.WorkflowRunner.Config
import com.squareup.workflow.ui.WorkflowRunnerViewModel.Factory
import com.squareup.workflow.ui.WorkflowRunnerViewModel.SnapshotSaver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowRunnerViewModelTest {

  private val testScope = CoroutineScope(Unconfined)
  private val runnerScope = testScope + Job(parent = testScope.coroutineContext[Job]!!)

  @After fun tearDown() {
    testScope.cancel()
  }

  @Test fun snapshotUpdatedOnHostEmission() {
    val snapshot1 = Snapshot.of("one")
    val snapshot2 = Snapshot.of("two")
    val outputDeferred = CompletableDeferred<String>()
    val renderingsAndSnapshots = MutableStateFlow(RenderingAndSnapshot(Any(), Snapshot.EMPTY))

    val runner = WorkflowRunnerViewModel(runnerScope, outputDeferred, renderingsAndSnapshots)

    assertThat(runner.getLastSnapshotForTest()).isEqualTo(Snapshot.EMPTY)

    renderingsAndSnapshots.value = RenderingAndSnapshot(Unit, snapshot1)
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot1)

    renderingsAndSnapshots.value = RenderingAndSnapshot(Unit, snapshot2)
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot2)
  }

  @Test fun `Factory cancels host on result and no sooner`() {
    val trigger = CompletableDeferred<String>()
    val workflow = Workflow.stateless<Unit, String, Unit> {
      runningWorker(trigger.asWorker()) { action { setOutput(it) } }
    }
    val runner = workflow.run()
    val tester = testScope.async { runner.awaitResult() }

    assertThat(tester.isActive).isTrue()
    trigger.complete("fnord")
    assertThat(tester.isCompleted).isTrue()
    assertThat(tester.getCompleted()).isEqualTo("fnord")
  }

  @Test fun `Factory cancels result when cleared`() {
    val workflow = Workflow.stateless<Unit, Unit, Unit> {}
    val runner = workflow.run()
    val tester = testScope.async { runner.awaitResult() }

    assertThat(tester.isActive).isTrue()
    runner.clearForTest()
    assertThat(tester.isCancelled).isTrue()
  }

  @Test fun `Factory cancels runtime when cleared`() {
    var cancelled = false
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      runningWorker(Worker.createSideEffect {
        suspendCancellableCoroutine {
          it.invokeOnCancellation {
            cancelled = true
          }
        }
      })
    }
    val runner = workflow.run()

    assertThat(cancelled).isFalse()
    runner.clearForTest()
    assertThat(cancelled).isTrue()
  }

  @Test fun hostCancelledOnCleared() {
    var cancellationException: Throwable? = null
    runnerScope.coroutineContext[Job]!!.invokeOnCompletion { e ->
      if (e is CancellationException) {
        cancellationException = e
      } else throw AssertionError(
          "Expected ${CancellationException::class.java.simpleName}", e
      )
    }
    val outputDeferred = CompletableDeferred<String>()
    val renderingsAndSnapshots = MutableStateFlow(RenderingAndSnapshot(Any(), Snapshot.EMPTY))
    val runner = WorkflowRunnerViewModel(runnerScope, outputDeferred, renderingsAndSnapshots)

    assertThat(cancellationException).isNull()
    runner.clearForTest()
    assertThat(cancellationException).isInstanceOf(CancellationException::class.java)
    val cause = generateSequence(cancellationException) { it.cause }
        .firstOrNull { it !is CancellationException }
    assertThat(cause).isNull()
  }

  @Test fun resultDelivered() {
    val outputs = BroadcastChannel<String>(1)
    val runner = Workflow
        .stateless<Unit, String, Unit> {
          runningWorker(outputs.asWorker()) { action { setOutput(it) } }
          Unit
        }
        .run()

    val tester = testScope.async { runner.awaitResult() }

    assertThat(tester.isActive).isTrue()
    runBlocking { outputs.send("fnord") }
    assertThat(tester.isCompleted).isTrue()
    assertThat(tester.getCompleted()).isEqualTo("fnord")
  }

  @Test fun resultCancelledOnCleared() {
    val runner = Workflow
        .stateless<Unit, String, Unit> {
          runningWorker(flowNever<String>().asWorker()) { action { setOutput(it) } }
        }
        .run()

    val tester = testScope.async { runner.awaitResult() }

    assertThat(tester.isActive).isTrue()
    runner.clearForTest()
    assertThat(tester.isCancelled).isTrue()
    assertThat(tester.getCompletionExceptionOrNull())
        .isInstanceOf(CancellationException::class.java)
    assertThat(tester.getCompletionCauseOrNull()).isNull()
  }

  private fun <O : Any, R : Any> Workflow<Unit, O, R>.run(): WorkflowRunnerViewModel<O> {
    @Suppress("UNCHECKED_CAST")
    return Factory(NoopSnapshotSaver) { Config(this, Unit, Unconfined) }
        .create(WorkflowRunnerViewModel::class.java) as WorkflowRunnerViewModel<O>
  }

  private fun <T> flowNever(): Flow<T> {
    return flow { suspendCancellableCoroutine { } }
  }

  private fun <T> Deferred<T>.getCompletionCauseOrNull() =
    generateSequence(getCompletionExceptionOrNull()) { it.cause }
        .firstOrNull { it !is CancellationException }

  object NoopSnapshotSaver : SnapshotSaver {
    override fun consumeSnapshot(): Snapshot? = null
    override fun registerProvider(provider: SavedStateProvider) = Unit
  }
}
