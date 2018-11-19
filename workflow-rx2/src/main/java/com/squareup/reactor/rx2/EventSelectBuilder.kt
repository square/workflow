package com.squareup.reactor.rx2

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.Single.just
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.selects.SelectBuilder
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

/**
 * The receiver for lambdas passed to [EventChannel.select][Rx2EventChannel.select].
 * For usage see the documentation for [EventChannel][Rx2EventChannel].
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
  private val context = Dispatchers.Unconfined + selectionJob

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
   * Defines a case that is selected when `maybe` completes successfully, and is passed the value
   * emitted by `maybe`.
   *
   * If `maybe` completes without a value, `handler` will never be invoked but the select expression
   * will continue to wait for any of its other cases.
   */
  fun <T : Any> onMaybeSuccessOrNever(
    maybe: Maybe<out T>,
    handler: (T) -> R
  ) {
    with(builder) {
      GlobalScope.async(context) { maybe.await() ?: suspendForever() }
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
    cases += SelectCase<E, T, R>(predicateMapper, handler)
  }
}

private suspend fun suspendForever(): Nothing = suspendCancellableCoroutine { }
