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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.rx2.asObservable
import kotlin.reflect.jvm.jvmName

@ExperimentalWorkflowUi
internal class WorkflowRunnerViewModel<OutputT : Any>(
  override val viewRegistry: ViewRegistry,
  workflowUpdates: WorkflowHost<OutputT, Any>
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
      return WorkflowRunnerViewModel(viewRegistry, workflowHost) as T
    }
  }

  private val subs = CompositeDisposable()

  @Suppress("EXPERIMENTAL_API_USAGE")
  private val updates =
    workflowUpdates.updates.asObservable(Dispatchers.Unconfined)
        .doOnNext { lastSnapshot = it.snapshot }
        .replay(1)
        .autoConnect(1) { subs.add(it) }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  override val renderings: Observable<out Any> = updates.map { it.rendering }

  override val output: Flowable<out OutputT> =
    // Buffer on backpressure so outputs don't get lost.
    updates.toFlowable(BUFFER)
        .filter { it.output != null }
        .map { it.output!! }
        // DON'T replay, outputs are events.
        .publish()
        // Subscribe upstream immediately so we immediately start getting notified about outputs.
        // If [renderings] triggers the upstream subscription before we do, if the workflow emits
        // an output immediately we might not see it.
        .autoConnect(0) { subs.add(it) }

  override fun onCleared() {
    // Has the side effect of closing the updates channel, which in turn
    // will fire any tear downs registered by the root workflow.
    subs.clear()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  private companion object {
    val BUNDLE_KEY = WorkflowRunner::class.jvmName + "-workflow"
  }
}
