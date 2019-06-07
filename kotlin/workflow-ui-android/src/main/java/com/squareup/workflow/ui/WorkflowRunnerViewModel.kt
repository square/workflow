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

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.jvm.jvmName

@ExperimentalWorkflowUi
@UseExperimental(ExperimentalCoroutinesApi::class)
internal class WorkflowRunnerViewModel<OutputT : Any>(
  override val viewRegistry: ViewRegistry,
  private val workflowHost: WorkflowHost<OutputT, Any>
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

  // TODO should inject a scope or use Main.immediate or something.
  private val snapshotUpdaterJob = GlobalScope.launch(Unconfined) {
    workflowHost.renderingsAndSnapshots
        .collect { lastSnapshot = it.snapshot }
  }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  override val renderings: Flow<Any> =
    workflowHost.renderingsAndSnapshots
        .map { it.rendering }

  override val output: Flow<OutputT> = workflowHost.outputs

  override fun onCleared() {
    workflowHost.cancel()
    snapshotUpdaterJob.cancel()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  private companion object {
    val BUNDLE_KEY = WorkflowRunner::class.jvmName + "-workflow"
  }
}
