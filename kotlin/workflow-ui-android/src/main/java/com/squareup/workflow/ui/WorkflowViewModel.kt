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
import com.squareup.workflow.WorkflowHost
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.asObservable

/**
 * The guts of [WorkflowActivityRunner]. We could have made that class itself a
 * [ViewModel], but that would allow accidental calls to [onCleared], which
 * would be nasty.
 */
internal class WorkflowViewModel<OutputT : Any, RenderingT : Any>(
  val viewRegistry: ViewRegistry,
  host: WorkflowHost<OutputT, RenderingT>
) : ViewModel() {

  internal class Factory<InputT : Any, OutputT : Any, RenderingT : Any>(
    private val viewRegistry: ViewRegistry,
    private val workflow: Workflow<InputT, OutputT, RenderingT>,
    private val initialInput: InputT,
    private val restored: PickledWorkflow?
  ) : ViewModelProvider.Factory {
    @Suppress("EXPERIMENTAL_API_USAGE")
    private val hostFactory = WorkflowHost.Factory(Dispatchers.Unconfined)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val host = hostFactory.run(workflow, initialInput, restored?.snapshot)
      @Suppress("UNCHECKED_CAST")
      return WorkflowViewModel(viewRegistry, host) as T
    }
  }

  private lateinit var sub: Disposable

  var lastSnapshot: Snapshot = Snapshot.EMPTY

  @Suppress("EXPERIMENTAL_API_USAGE")
  val updates =
    host.updates.asObservable(Dispatchers.Unconfined)
        // Issue #252, try replacing this with Dispatchers.Main.immediate once that's available.
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { lastSnapshot = it.snapshot }
        .replay(1)
        .autoConnect(1) { sub = it }

  override fun onCleared() {
    // Has the side effect of closing the updates channel, which in turn
    // will fire any tear downs registered by the root workflow.
    sub.dispose()
  }
}
