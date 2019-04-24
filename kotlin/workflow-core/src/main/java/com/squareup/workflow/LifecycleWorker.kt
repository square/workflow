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
package com.squareup.workflow

import com.squareup.workflow.Worker.Emitter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * [Worker] that performs some action when the worker is started and/or cancelled.
 */
abstract class LifecycleWorker : Worker<Nothing> {

  /**
   * Called when this worker is started.
   *
   * It will be invoked on the dispatcher running the workflow, inside a [NonCancellable] block.
   */
  open suspend fun onStarted() {}

  /**
   * Called when this worker has been torn down.
   *
   * It will be invoked on the dispatcher running the workflow, inside a [NonCancellable] block.
   */
  open suspend fun onCancelled() {}

  final override suspend fun performWork(emitter: Emitter<Nothing>) {
    withContext(NonCancellable) {
      onStarted()
    }

    try {
      // Hang forever, or until this coroutine is cancelled.
      suspendCancellableCoroutine<Nothing> { }
    } finally {
      withContext(NonCancellable) {
        onCancelled()
      }
    }
  }

  /**
   * Equates [LifecycleWorker]s that have the same concrete class.
   */
  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    this::class == otherWorker::class
}
