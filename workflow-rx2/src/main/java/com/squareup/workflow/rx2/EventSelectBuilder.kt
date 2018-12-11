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
package com.squareup.workflow.rx2

import io.reactivex.Single
import io.reactivex.Single.just
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.selects.SelectBuilder

/**
 * The receiver for lambdas passed to [EventChannel.select][EventChannel.select].
 * For usage see the documentation for [EventChannel][EventChannel].
 */
class EventSelectBuilder<E : Any, R : Any> internal constructor(
  private val builder: SelectBuilder<Single<R>>,
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
   * Note that this class intentionally does not implement `CoroutineScope`, since we don't want to
   * expose this context to callers and suggest that the `select` block is intended to be used to
   * start coroutines.
   */
  private val context = Unconfined + selectionJob

  /** Selects an event by type `T`. */
  inline fun <reified T : E> onEvent(noinline handler: (T) -> R) {
    addEventCase({ it as? T }, handler)
  }

  /**
   * Defines a case that is selected when `single` completes successfully, and is passed the value
   * emitted by `single`.
   */
  fun <T : Any> onSuccess(
    single: Single<out T>,
    handler: (T) -> R
  ) {
    with(builder) {
      GlobalScope.async(context) { single.await() }
          .onAwait { just(handler(it)) }
    }
  }

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
}
