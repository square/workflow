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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

internal class WorkflowRunnerViewModel<OutputT : Any>(
  private val scope: CoroutineScope,
  private val session: WorkflowSession<OutputT, Any>
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
        WorkflowRunnerViewModel(this, session).apply {
          savedStateRegistry.registerSavedStateProvider(BUNDLE_KEY, this)
        } as T
      }
    }
  }

  private val result = scope.async {
    session.outputs.first()
  }

  override suspend fun awaitResult(): OutputT = result.await()

  init {
    @Suppress("EXPERIMENTAL_API_USAGE")
    session.renderingsAndSnapshots
        .map { it.snapshot }
        .onEach { lastSnapshot = it }
        .launchIn(scope)

    // Cancel the entire workflow runtime after the first output is emitted.
    // Use the Unconfined dispatcher to ensure the cancellation happens as immediately as possible.
    scope.launch(Dispatchers.Unconfined) {
      result.join()
      scope.cancel(CancellationException("WorkflowRunnerViewModel delivered result"))
    }
  }

  private var lastSnapshot: Snapshot = Snapshot.EMPTY

  @OptIn(ExperimentalCoroutinesApi::class)
  override val renderings: Flow<Any> = session.renderingsAndSnapshots
      .map { it.rendering }

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
