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
import androidx.lifecycle.viewModelScope
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.launchWorkflowIn
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.jvm.jvmName

@UseExperimental(ExperimentalCoroutinesApi::class)
@ExperimentalWorkflowUi
internal class WorkflowRunnerViewModel<OutputT : Any>(
  override val viewRegistry: ViewRegistry,
  private val renderingsFlow: Flow<RenderingAndSnapshot<Any>>,
  outputsFlow: Flow<OutputT>,
  private val workflowJob: Job
) : ViewModel(), WorkflowRunner<OutputT> {

  /**
   * @param inputs Function that returns a channel that delivers input values for the root
   * workflow. The first value emitted is passed to `initialState` to determine the root
   * workflow's initial state, and subsequent emissions are passed as input updates to the root
   * workflow. The channel returned by this function will be cancelled by the host when it's
   * finished.
   */
  @UseExperimental(ExperimentalCoroutinesApi::class)
  internal class Factory<InputT, OutputT : Any>(
    private val workflow: Workflow<InputT, OutputT, Any>,
    private val viewRegistry: ViewRegistry,
    private val inputs: Flow<InputT>,
    savedInstanceState: Bundle?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
  ) : ViewModelProvider.Factory {
    private val snapshot = savedInstanceState
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T = launchWorkflowIn(
        CoroutineScope(dispatcher),
        workflow,
        inputs,
        snapshot
    ) { renderings, outputs ->
      @Suppress("UNCHECKED_CAST")
      WorkflowRunnerViewModel(viewRegistry, renderings, outputs, coroutineContext[Job]!!) as T
    }
  }

  init {
    viewModelScope.coroutineContext[Job]!!.invokeOnCompletion {
      workflowJob.cancel(CancellationException("WorkflowRunnerViewModel cancelled.", it))
    }

    viewModelScope.launch {
      renderingsFlow
          .map { it.snapshot }
          .collect { lastSnapshot = it }
    }
  }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override val renderings: Observable<out Any> = renderingsFlow
      .map { it.rendering }
      .asObservable()

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override val output: Flowable<out OutputT> = outputsFlow
      .asFlowable()

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  @TestOnly
  internal fun getLastSnapshotForTest() = lastSnapshot

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
