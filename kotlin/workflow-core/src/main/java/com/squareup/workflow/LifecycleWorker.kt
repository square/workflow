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
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [Worker] that performs some action when the worker is started and/or cancelled.
 */
abstract class LifecycleWorker : Worker<Nothing> {

  /**
   * Called when this worker is started. It is executed concurrently with the parent workflow –
   * the first render pass that starts this worker *will not* wait for this method to return, and
   * one or more additional render passes may occur before this method is called.
   * This behavior may change to be more strict in the future.
   *
   * This method will be called exactly once for each matching call to [onCancelled].
   *
   * Invoked on the dispatcher running the workflow.
   */
  open fun onStarted() {}

  /**
   * Called when this worker has been torn down. It is executed concurrently with the parent
   * workflow – the render pass that cancels this worker *will not* wait for this method to return,
   * and one or more additional render passes may occur before this method is called.
   * This behavior may change to be more strict in the future.
   *
   * This method will be called exactly once for each matching call to [onStarted].
   *
   * Invoked on the dispatcher running the workflow.
   */
  open fun onCancelled() {}

  final override suspend fun performWork(emitter: Emitter<Nothing>) {
    onStarted()

    try {
      // Hang forever, or until this coroutine is cancelled.
      // Don't use CancellableContinuation.invokeOnCancellation because it doesn't have any
      // guarantees about which thread it's run on. Using try/finally means the cancellation action
      // doesn't block the cancellation, but ensures it's run on the correct dispatcher.
      suspendCancellableCoroutine<Nothing> { }
    } finally {
      onCancelled()
    }
  }

  /**
   * Equates [LifecycleWorker]s that have the same concrete class.
   */
  override fun doesSameWorkAs(otherWorker: Worker<*>): Boolean =
    this::class == otherWorker::class
}
