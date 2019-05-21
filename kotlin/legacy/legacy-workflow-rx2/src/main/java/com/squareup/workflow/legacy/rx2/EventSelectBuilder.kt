/*
 * Copyright 2017 Square Inc.
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
@file:Suppress("DeprecatedCallableAddReplaceWith", "EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.squareup.workflow.legacy.Finished
import com.squareup.workflow.legacy.Running
import com.squareup.workflow.legacy.Worker
import com.squareup.workflow.legacy.Workflow
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.WorkflowUpdate
import io.reactivex.Single
import io.reactivex.Single.just
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.selects.SelectBuilder

/**
 * The receiver for lambdas passed to [EventChannel.select][EventChannel.select].
 * For usage see the documentation for [EventChannel][EventChannel].
 */
@Deprecated("Use com.squareup.workflow.Workflow")
class EventSelectBuilder<E : Any, R : Any> internal constructor(
  @PublishedApi internal val builder: SelectBuilder<Single<R>>,
  /**
   * Job that should be used as the parent for any coroutines started to wait for potential inputs.
   * This job will be cancelled once a selection is made.
   */
  selectionJob: Job
) {

  /**
   * Describes a particular type of event to watch for.
   *
   * @param predicateMapper defines when this case should be selected, and how to convert an event
   * to the specific type expected by [handler].
   * @param handler the block of code that is evaluated if this case is selected, and whose return
   * value is emitted from the `select`'s `Single`.
   */
  internal class SelectCase<E : Any, T : E, R>(
    val predicateMapper: (E) -> T?,
    val handler: (T) -> R
  ) {
    /**
     * Given [event], if [predicateMapper] returns non-null for [event], returns a function that will
     * invoke [handler] with the result from [predicateMapper].
     *
     * This allows code to interact with cases with different values for [T] in a type-safe manner.
     */
    fun tryHandle(event: E) = predicateMapper(event)?.let { { handler(it) } }
  }

  internal val cases: MutableList<SelectCase<E, *, R>> = mutableListOf()

  /**
   * Context that should be used for all coroutines started to wait for potential inputs.
   * It's job will be cancelled once a selection is made.
   * Note that [EventSelectBuilder] intentionally does not implement `CoroutineScope`, since we
   * don't want to expose this context to callers and suggest that the `select` block is intended
   * to be used to start coroutines.
   */
  @PublishedApi
  internal val scope = CoroutineScope(Unconfined + selectionJob)

  /** Selects an event by type `T`. */
  inline fun <reified T : E> onEvent(noinline handler: (T) -> R) {
    addEventCase({ it as? T }, handler)
  }

  /**
   * Starts the [Workflow] described by [handle] if it wasn't already running.
   * This case is selected when the [Workflow]'s state changes from the
   * [given][WorkflowPool.Handle.state], or the [Workflow] completes.
   *
   * Callers are responsible for keeping the [Workflow] running until it is
   * [Finished], or else [abandoning][WorkflowPool.abandonWorkflow] it. To keep the [Workflow]
   * running, call [onWorkflowUpdate] again with the provided [Running.handle].
   *
   * If the workflow is [abandoned][WorkflowPool.abandonWorkflow], this case will never
   * be selected.
   */
  fun <S : Any, E : Any, O : Any> WorkflowPool.onWorkflowUpdate(
    handle: WorkflowPool.Handle<S, E, O>,
    handler: (WorkflowUpdate<S, E, O>) -> R
  ) = onSuspending(handler) {
    awaitWorkflowUpdate(handle)
  }

  /**
   * Selected when the given [worker] produces its result. If [worker] wasn't already running,
   * it is started the given [input]. The caller must ensure that the result is consumed, or
   * else call [WorkflowPool.abandonWorker].
   *
   * This method can be called with the same worker multiple times and it will only be started once,
   * until it finishes. Then, the next time it is called it will restart the worker.
   *
   * If the nested workflow is [abandoned][WorkflowPool.abandonWorker], this case will never
   * be selected.
   *
   * @see WorkflowPool.awaitWorkerResult
   */
  inline fun <reified I : Any, reified O : Any> WorkflowPool.onWorkerResult(
    worker: Worker<I, O>,
    input: I,
    name: String = "",
    noinline handler: (O) -> R
  ) = onSuspending(handler) {
    awaitWorkerResult(worker, input, name)
  }

  /**
   * Defines a case that is selected when `single` completes successfully, and is passed the value
   * emitted by `single`.
   */
  @Deprecated("Create a Worker and use onNextDelegateReaction or onWorkerResult instead.")
  fun <T : Any> onSuccess(
    single: Single<out T>,
    handler: (T) -> R
  ) = onSuspending(handler) { single.await() }

  /**
   * Selects an event of type `eventClass` that also satisfies `predicate`.
   */
  @PublishedApi internal fun <T : E> addEventCase(
    predicateMapper: (E) -> T?,
    handler: (T) -> R
  ) {
    cases += SelectCase<E, T, R>(
        predicateMapper, handler
    )
  }

  /**
   * Starts a new coroutine to run the [block] and then selects on the `block`'s result.
   *
   * This should **not** be used to await on types that have built-in support for `select`, since
   * selection is not atomic. For things that don't support select there is a race between
   * completion and the other select cases being selected. However, this shouldn't be a problem in
   * practice because [EventSelectBuilder] makes no guarantees about thread safety and so it already
   * relies on external synchronization.
   */
  @PublishedApi internal fun <T> onSuspending(
    handler: (T) -> R,
    block: suspend () -> T
  ) {
    with(builder) {
      scope.async(start = UNDISPATCHED) { block() }
          .onAwait { just(handler(it)) }
    }
  }
}
