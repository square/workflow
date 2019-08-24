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
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.launchWorkflowIn
import io.reactivex.Maybe
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.rx2.asObservable
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CancellationException

@UseExperimental(ExperimentalCoroutinesApi::class)
internal class WorkflowRunnerViewModel<OutputT : Any>(
  private val scope: CoroutineScope,
  renderingsFlow: Flow<RenderingAndSnapshot<Any>>,
  output: Flow<OutputT>,
  override val viewRegistry: ViewRegistry
) : ViewModel(), WorkflowRunner<OutputT> {

  @UseExperimental(ExperimentalCoroutinesApi::class)
  internal class Factory<PropsT, OutputT : Any>(
    savedInstanceState: Bundle?,
    private val configure: () -> WorkflowRunner.Config<PropsT, OutputT>
  ) : ViewModelProvider.Factory {
    private val snapshot = savedInstanceState
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return with(configure()) {
        launchWorkflowIn(
            CoroutineScope(dispatcher), workflow, props, snapshot
        ) { renderings, output ->
          @Suppress("UNCHECKED_CAST")
          WorkflowRunnerViewModel(this, renderings, output, viewRegistry) as T
        }
      }
    }
  }

  override val result: Maybe<out OutputT> = output.asObservable()
      .firstElement()
      .doAfterTerminate {
        scope.cancel(CancellationException("WorkflowRunnerViewModel delivered result"))
      }
      .cache()

  init {
    renderingsFlow
        .map { it.snapshot }
        .onEach { lastSnapshot = it }
        .launchIn(scope)
  }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  @UseExperimental(ExperimentalCoroutinesApi::class)
  override val renderings: Observable<out Any> = renderingsFlow
      .map { it.rendering }
      .asObservable()

  override fun onCleared() {
    scope.cancel(CancellationException("WorkflowRunnerViewModel cleared."))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  @TestOnly
  internal fun clearForTest() = onCleared()

  @TestOnly
  internal fun getLastSnapshotForTest() = lastSnapshot

  private companion object {
    val BUNDLE_KEY = WorkflowRunner::class.java.name + "-workflow"
  }
}
