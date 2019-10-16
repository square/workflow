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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import com.squareup.workflow.Snapshot
import com.squareup.workflow.WorkflowSession
import com.squareup.workflow.launchWorkflowIn
import com.squareup.workflow.ui.WorkflowRunner.Config
import io.reactivex.Maybe
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.rx2.asObservable
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CancellationException

@UseExperimental(ExperimentalCoroutinesApi::class)
internal class WorkflowRunnerViewModel<OutputT : Any>(
  private val scope: CoroutineScope,
  session: WorkflowSession<OutputT, Any>,
  override val viewRegistry: ViewRegistry
) : ViewModel(), WorkflowRunner<OutputT>, SavedStateProvider {

  internal class Factory<PropsT, OutputT : Any>(
    private val savedStateRegistry: SavedStateRegistry,
    private val configure: () -> Config<PropsT, OutputT>
  ) : ViewModelProvider.Factory {
    private val snapshot = savedStateRegistry
        .consumeRestoredStateForKey(BUNDLE_KEY)
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val config = configure()
      return launchWorkflowIn(
          CoroutineScope(config.dispatcher), config.workflow, config.props, snapshot
      ) { session ->
        session.diagnosticListener = config.diagnosticListener
        @Suppress("UNCHECKED_CAST")
        WorkflowRunnerViewModel(this, session, config.viewRegistry).apply {
          savedStateRegistry.registerSavedStateProvider(BUNDLE_KEY, this)
        } as T
      }
    }
  }

  override val result: Maybe<out OutputT> = session.outputs.asObservable()
      .firstElement()
      .doAfterTerminate {
        scope.cancel(CancellationException("WorkflowRunnerViewModel delivered result"))
      }
      .cache()

  init {
    session.renderingsAndSnapshots
        .map { it.snapshot }
        .onEach { lastSnapshot = it }
        .launchIn(scope)
  }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  override val renderings: Observable<out Any> = session.renderingsAndSnapshots
      .map { it.rendering }
      .asObservable()

  override fun onCleared() {
    scope.cancel(CancellationException("WorkflowRunnerViewModel cleared."))
  }

  override fun saveState() = Bundle().apply {
    putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  @TestOnly
  internal fun clearForTest() = onCleared()

  @TestOnly
  internal fun getLastSnapshotForTest() = lastSnapshot

  private companion object {
    /**
     * Namespace key, used in two namespaces:
     *  - associates the [WorkflowRunnerViewModel] with the [SavedStateRegistry]
     *  - and is also the key for the [PickledWorkflow] in the bundle created by [saveState].
     */
    val BUNDLE_KEY = WorkflowRunner::class.java.name + "-workflow"
  }
}
