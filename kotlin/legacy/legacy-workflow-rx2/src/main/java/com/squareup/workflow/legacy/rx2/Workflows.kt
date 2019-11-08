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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.squareup.workflow.legacy.Workflow
import io.reactivex.Maybe
import io.reactivex.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.rx2.rxMaybe
import kotlinx.coroutines.rx2.rxObservable

/**
 * On every update, reports the complete, current state of this workflow.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
val <S : Any> Workflow<S, *, *>.state: Observable<out S>
  get() = rxObservable(Unconfined) {
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
@Deprecated("Use com.squareup.workflow.Workflow")
val <O : Any> Workflow<*, *, O>.result: Maybe<out O>
  get() = rxMaybe(Unconfined) { await() }
      .onErrorResumeNext { error: Throwable ->
        if (error is CancellationException) {
          Maybe.empty()
        } else {
          Maybe.error(error)
        }
      }
