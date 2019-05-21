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
import com.squareup.workflow.WorkflowHost.Update
import com.squareup.workflow.rx2.flatMapWorkflow
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.jvm.jvmName

/**
 * The guts of [WorkflowActivityRunner] and [WorkflowFragment].
 */
@ExperimentalWorkflowUi
internal class WorkflowRunnerViewModel<OutputT : Any>(
  override val viewRegistry: ViewRegistry,
  workflowUpdates: Flowable<Update<OutputT, Any>>
) : ViewModel(), WorkflowRunner<OutputT> {

  internal class Factory<InputT, OutputT : Any>(
    private val workflow: Workflow<InputT, OutputT, Any>,
    private val viewRegistry: ViewRegistry,
    private val inputs: Flowable<InputT>,
    savedInstanceState: Bundle?
  ) : ViewModelProvider.Factory {
    private val snapshot = savedInstanceState
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val workflowUpdates = inputs.flatMapWorkflow(workflow, snapshot, Dispatchers.Main.immediate)
      @Suppress("UNCHECKED_CAST")
      return WorkflowRunnerViewModel(viewRegistry, workflowUpdates) as T
    }
  }

  private lateinit var sub: Disposable

  @Suppress("EXPERIMENTAL_API_USAGE")
  private val updates =
    workflowUpdates.toObservable()
        .doOnNext { lastSnapshot = it.snapshot }
        .replay(1)
        .autoConnect(1) { sub = it }

  var lastSnapshot: Snapshot = Snapshot.EMPTY

  override val renderings: Observable<out Any> = updates.map { it.rendering }

  override val output: Observable<out OutputT> = updates.filter { it.output != null }
      .map { it.output!! }

  override fun onCleared() {
    // Has the side effect of closing the updates channel, which in turn
    // will fire any tear downs registered by the root workflow.
    sub.dispose()
  }

  fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  private companion object {
    val BUNDLE_KEY = WorkflowActivityRunner::class.jvmName + "-workflow"
  }
}
