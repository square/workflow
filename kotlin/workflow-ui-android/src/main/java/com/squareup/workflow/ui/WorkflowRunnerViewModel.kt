/*
 * Copyright 2019 Square Inc.
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
package com.squareup.workflow.ui

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.jvm.jvmName

@UseExperimental(ExperimentalCoroutinesApi::class)
@ExperimentalWorkflowUi
internal class WorkflowRunnerViewModel<OutputT : Any>(
  override val viewRegistry: ViewRegistry,
  private val workflowHost: WorkflowHost<OutputT, Any>,
  private val context: CoroutineContext
) : ViewModel(), WorkflowRunner<OutputT> {

  /**
   * @param inputs Function that returns a channel that delivers input values for the root
   * workflow. The first value emitted is passed to `initialState` to determine the root
   * workflow's initial state, and subsequent emissions are passed as input updates to the root
   * workflow. The channel returned by this function will be cancelled by the host when it's
   * finished.
   */
  internal class Factory<InputT, OutputT : Any> constructor(
    private val workflow: Workflow<InputT, OutputT, Any>,
    private val viewRegistry: ViewRegistry,
    private val inputs: () -> ReceiveChannel<InputT>,
    savedInstanceState: Bundle?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
  ) : ViewModelProvider.Factory {
    private val snapshot = savedInstanceState
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val hostFactory = WorkflowHost.Factory(dispatcher)
      val workflowHost = hostFactory.run(workflow, inputs, snapshot)
      @Suppress("UNCHECKED_CAST")
      return WorkflowRunnerViewModel(viewRegistry, workflowHost, dispatcher) as T
    }
  }

  private val snapshotJob = GlobalScope.launch(context) {
    workflowHost.renderingsAndSnapshots
        .map { it.snapshot }
        .collect { lastSnapshot = it }
  }

  private var workflowJob: Job? = null

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override val renderings: Observable<out Any> = workflowHost.renderingsAndSnapshots
      .onCollect { startWorkflowHost() }
      .map { it.rendering }
      .asObservable()

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override val output: Flowable<out OutputT> = workflowHost.outputs
      .onCollect { startWorkflowHost() }
      .asFlowable()

  override fun onCleared() {
    val cancellationException = CancellationException("WorkflowRunnerViewModel cleared.")
    snapshotJob.cancel(cancellationException)
    workflowJob?.cancel(cancellationException)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  @TestOnly
  internal fun clearForTest() = onCleared()

  @TestOnly
  internal fun getLastSnapshotForTest() = lastSnapshot

  /**
   * Starts the [workflowHost] and saves its job to [workflowJob].
   *
   * This call is idempotent and will always return the same Job, so we can just call it
   * every time. And so by making this call inside of `onCollect()`, we basically get the
   * nice effects of Rx's `autoConnect(1)` start up a shared stream the the first time
   * someone "subscribes" / collects.
   */
  private fun startWorkflowHost() {
    workflowJob = workflowHost.start()
  }

  private companion object {
    val BUNDLE_KEY = WorkflowRunner::class.jvmName + "-workflow"
  }
}

/**
 * Invokes `block` every time a new collector begins collecting this [Flow].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
private fun <T> Flow<T>.onCollect(block: () -> Unit): Flow<T> = flow {
  block()
  emitAll(this@onCollect)
}
