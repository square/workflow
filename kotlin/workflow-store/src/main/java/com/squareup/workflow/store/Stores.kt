/*
 * Copyright 2020 Square Inc.
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
package com.squareup.workflow.store

import com.dropbox.android.external.store4.ResponseOrigin
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreRequest
import com.dropbox.android.external.store4.StoreResponse
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.action
import com.squareup.workflow.asWorker
import com.squareup.workflow.store.StoreWorkflow.State

private val DEFAULT_RESPONSE = StoreResponse.Loading<Nothing>(ResponseOrigin.Cache)

/**
 * Returns a [Workflow] that will query a [Store] for they [StoreRequest] passed as props, and
 * return the [StoreResponse] as its rendering.
 *
 * Example:
 * ```
 * val response = context.render(store.workflow(), StoreRequest.cached("key"))
 * when(response) {
 *   is StoreResponse.Loading -> TODO()
 *   is StoreResponse.Data -> TODO()
 *   is StoreResponse.Error -> TODO()
 * }
 * ```
 */
fun <Key : Any, Output : Any> Store<Key, Output>.workflow():
    Workflow<StoreRequest<Key>, Nothing, StoreResponse<Output>> = StoreWorkflow(this)

internal class StoreWorkflow<Key : Any, Output : Any>(
  private val store: Store<Key, Output>
) : StatefulWorkflow<StoreRequest<Key>, State<Output>, Nothing, StoreResponse<Output>>() {

  @Suppress("EqualsOrHashCode")
  data class State<out Output>(
    val worker: Worker<StoreResponse<Output>>,
    val lastResponse: StoreResponse<Output> = DEFAULT_RESPONSE
  )

  override fun initialState(
    props: StoreRequest<Key>,
    snapshot: Snapshot?
  ) = State(worker = store.streamWorker(props))

  override fun onPropsChanged(
    old: StoreRequest<Key>,
    new: StoreRequest<Key>,
    state: State<Output>
  ): State<Output> = if (old != new) state.copy(worker = store.streamWorker(new)) else state

  override fun render(
    props: StoreRequest<Key>,
    state: State<Output>,
    context: RenderContext<State<Output>, Nothing>
  ): StoreResponse<Output> {
    context.runningWorker(state.worker, handler = ::handleResponse)
    return state.lastResponse
  }

  override fun snapshotState(state: State<Output>): Snapshot = Snapshot.EMPTY

  private fun handleResponse(response: StoreResponse<Output>) = action {
    nextState = nextState.copy(lastResponse = response)
  }
}

private fun <Key : Any, Output : Any> Store<Key, Output>.streamWorker(
  request: StoreRequest<Key>
) = stream(request)
    .asWorker()
