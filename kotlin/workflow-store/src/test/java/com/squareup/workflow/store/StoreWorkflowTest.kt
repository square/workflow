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

import com.dropbox.android.external.store4.ExperimentalStoreApi
import com.dropbox.android.external.store4.ResponseOrigin.Cache
import com.dropbox.android.external.store4.ResponseOrigin.Fetcher
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreRequest
import com.dropbox.android.external.store4.StoreResponse
import com.squareup.workflow.store.StoreWorkflow.State
import com.squareup.workflow.testing.EmittedOutput
import com.squareup.workflow.testing.renderTester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoreWorkflowTest {

  private val store = TestStore()
  private val workflow = StoreWorkflow(store)

  @Test fun rendersInitialLoading() {
    workflow.renderTester(StoreRequest.cached("key", refresh = true))
        .expectWorker({ true })
        .render { rendering ->
          assertEquals(StoreResponse.Loading(Cache), rendering)
        }
  }

  @Test fun updatesOnNewResponse() {
    val newResponse = StoreResponse.Data("data", Fetcher)
    workflow.renderTester(StoreRequest.cached("key", refresh = true))
        .expectWorker({ true }, output = EmittedOutput(newResponse))
        .render { rendering ->
          assertEquals(StoreResponse.Loading(Cache), rendering)
        }
        .verifyActionResult { state, output ->
          val stateResponse = (state as State<*>).lastResponse
          assertEquals(newResponse, stateResponse)
          assertNull(output)
        }
  }

  @Test fun requeriesOnNewRequest() {
    TODO()
  }
}

private class TestStore : Store<String, String> {

  override fun stream(request: StoreRequest<String>): Flow<StoreResponse<String>> {
    return flow { TODO("not implemented") }
  }

  override suspend fun clear(key: String) {
    throw UnsupportedOperationException()
  }

  @ExperimentalStoreApi override suspend fun clearAll() {
    throw UnsupportedOperationException()
  }
}
