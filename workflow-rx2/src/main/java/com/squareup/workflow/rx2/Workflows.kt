package com.squareup.workflow.rx2

import com.squareup.workflow.Workflow
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.rx2.rxMaybe
import kotlinx.coroutines.experimental.rx2.rxObservable

/**
 * On every update, reports the complete, current state of this workflow.
 */
val <S : Any> Workflow<S, *, *>.state: Observable<out S>
  get() = GlobalScope
      .rxObservable(Unconfined) {
        val stateChannel = openSubscriptionToState()
        // This will cancel the channel on error or successful completion, which is necessary
        // to unsubscribe from the workflow.
        stateChannel.consumeEach { state ->
          send(state)
        }
      }
      .onErrorResumeNext { error: Throwable ->
        // When a coroutine throws a CancellationException, it's not actually an error, just a
        // a signal that the coroutine was cancelled. For workflows, it means the workflow was
        // abandoned.
        if (error is CancellationException) {
          Observable.empty()
        } else {
          Observable.error(error)
        }
      }
      .replay(1)
      .refCount()

/**
 * Called with the final result of this workflow. The result value should be cached – that is,
 * the result should be stored and emitted even if the [Maybe] isn't subscribed to until
 * after the workflow completes. It should also support multiple subscribers.
 *
 * If the workflow is abandoned, the result will complete without a value.
 */
val <O : Any> Workflow<*, *, O>.result: Maybe<out O>
  get() = GlobalScope
      .rxMaybe(Unconfined) { await() }
      .onErrorResumeNext { error: Throwable ->
        if (error is CancellationException) {
          Maybe.empty()
        } else {
          Maybe.error(error)
        }
      }

/**
 * Returns a [Completable] that fires either when [Workflow.result] fires, or when
 * [Workflow.cancel] is called.
 */
fun Workflow<*, *, *>.toCompletable(): Completable = state.ignoreElements()
