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
import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost.Update
import com.squareup.workflow.rx2.flatMapWorkflow
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable

/**
 * The guts of [WorkflowActivityRunner]. We could have made that class itself a
 * [ViewModel], but that would allow accidental calls to [onCleared], which
 * would be nasty.
 */
@ExperimentalWorkflowUi
internal class WorkflowViewModel<OutputT : Any, RenderingT>(
  val viewRegistry: ViewRegistry,
  workflowUpdates: Flowable<Update<OutputT, RenderingT>>
) : ViewModel() {

  internal class Factory<InputT, OutputT : Any, RenderingT>(
    private val viewRegistry: ViewRegistry,
    private val workflow: Workflow<InputT, OutputT, RenderingT>,
    private val inputs: Flowable<InputT>,
    private val restored: PickledWorkflow?
  ) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val workflowUpdates = inputs.flatMapWorkflow(workflow, restored?.snapshot)
      @Suppress("UNCHECKED_CAST")
      return WorkflowViewModel(viewRegistry, workflowUpdates) as T
    }
  }

  private lateinit var sub: Disposable

  var lastSnapshot: Snapshot = Snapshot.EMPTY

  @Suppress("EXPERIMENTAL_API_USAGE")
  val updates =
    workflowUpdates.toObservable()
        .doOnNext { lastSnapshot = it.snapshot }
        .replay(1)
        .autoConnect(1) { sub = it }

  override fun onCleared() {
    // Has the side effect of closing the updates channel, which in turn
    // will fire any tear downs registered by the root workflow.
    sub.dispose()
  }
}
