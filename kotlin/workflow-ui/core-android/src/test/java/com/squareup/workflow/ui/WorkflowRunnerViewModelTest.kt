package com.squareup.workflow.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.google.common.truth.Truth.assertThat
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowSession
import com.squareup.workflow.action
import com.squareup.workflow.asWorker
import com.squareup.workflow.stateless
import com.squareup.workflow.ui.WorkflowRunner.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
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
    val snapshotsChannel = Channel<RenderingAndSnapshot<Unit>>(UNLIMITED)
    val snapshotsFlow = flow { snapshotsChannel.consumeEach { emit(it) } }
    val session = WorkflowSession(
        renderingsAndSnapshots = snapshotsFlow,
        // Outputs should never complete, it can only fail.
        outputs = flowNever()
    )

    val runner = WorkflowRunnerViewModel(runnerScope, session)

    assertThat(runner.getLastSnapshotForTest()).isEqualTo(Snapshot.EMPTY)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot1))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot1)

    snapshotsChannel.offer(RenderingAndSnapshot(Unit, snapshot2))
    assertThat(runner.getLastSnapshotForTest()).isEqualTo(snapshot2)
  }

  @Test fun hostCancelledOnResultAndNoSooner() {
    var cancelled = false
    runnerScope.coroutineContext[Job]!!.invokeOnCompletion { e ->
      if (e is CancellationException) {
        cancelled = true
      } else throw AssertionError(
          "Expected ${CancellationException::class.java.simpleName}", e
      )
    }
    val outputChannel = Channel<String>(1)
    val session = WorkflowSession<String, Nothing>(
        renderingsAndSnapshots = emptyFlow(),
        outputs = outputChannel.consumeAsFlow()
    )
    val runner = WorkflowRunnerViewModel(runnerScope, session)
    val tester = testScope.async { runner.awaitResult() }

    assertThat(cancelled).isFalse()
    assertThat(tester.isActive).isTrue()

    outputChannel.offer("fnord")

    assertThat(cancelled).isTrue()
    assertThat(tester.isCompleted).isTrue()
    assertThat(tester.getCompleted()).isEqualTo("fnord")
  }

  @Test fun hostCancelledOnCleared() {
    var cancelled = false
    runnerScope.coroutineContext[Job]!!.invokeOnCompletion { e ->
      if (e is CancellationException) {
        cancelled = true
      } else throw AssertionError(
          "Expected ${CancellationException::class.java.simpleName}", e
      )
    }
    val session = WorkflowSession<Nothing, Nothing>(
        renderingsAndSnapshots = emptyFlow(),
        // Outputs should never complete, it can only fail.
        outputs = flowNever()
    )
    val runner = WorkflowRunnerViewModel(runnerScope, session)
    @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
    val tester = testScope.async { runner.awaitResult() }

    assertThat(cancelled).isFalse()
    runner.clearForTest()
    assertThat(cancelled).isTrue()
    assertThat(tester.isCancelled)
    assertThat(tester.getCompletionExceptionOrNull())
        .isInstanceOf(CancellationException::class.java)
    assertThat(tester.getCompletionCauseOrNull()).isNull()
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
    val registryOwner = object : SavedStateRegistryOwner {
      lateinit var controller: SavedStateRegistryController

      override fun getLifecycle(): Lifecycle {
        return object : Lifecycle() {
          override fun addObserver(observer: LifecycleObserver) = Unit
          override fun removeObserver(observer: LifecycleObserver) = Unit
          override fun getCurrentState() = State.INITIALIZED
        }
      }

      override fun getSavedStateRegistry(): SavedStateRegistry {
        return controller.savedStateRegistry
      }
    }

    registryOwner.controller = SavedStateRegistryController.create(registryOwner)
        .apply { performRestore(null) }

    @Suppress("UNCHECKED_CAST")
    return WorkflowRunnerViewModel
        .Factory(registryOwner.savedStateRegistry) {
          Config(this, Unit, Unconfined)
        }
        .create(WorkflowRunnerViewModel::class.java) as WorkflowRunnerViewModel<O>
  }

  private fun <T> flowNever(): Flow<T> {
    return flow { suspendCancellableCoroutine { } }
  }

  private fun <T> Deferred<T>.getCompletionCauseOrNull() =
    generateSequence(getCompletionExceptionOrNull()) { it.cause }
        .firstOrNull { it !is CancellationException }
}
