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
@file:JvmName("WorkflowPools")

package com.squareup.workflow.rx2

import com.squareup.workflow.Delegating
import com.squareup.workflow.Worker
import com.squareup.workflow.Reaction
import com.squareup.workflow.WorkflowPool
import io.reactivex.Single
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.rx2.asSingle
import com.squareup.workflow.nextDelegateReaction as nextDelegateReactionCore
import com.squareup.workflow.workerResult as workerResultCore

/**
 * Starts the required nested workflow if it wasn't already running. Returns
 * a [Single] that will fire the next time the nested workflow updates its state,
 * or completes.
 * States that are equal to the [Delegating.delegateState] are skipped.
 *
 * If the nested workflow was not already running, it is started in the
 * [given state][Delegating.delegateState] (the initial state is not reported, since states equal
 * to the delegate state are skipped). Otherwise, the [Single] skips state updates that match the
 * given state.
 *
 * If the nested workflow is [abandoned][WorkflowPool.abandonDelegate], the [Single] never
 * completes.
 */
fun <S : Any, O : Any> WorkflowPool.nextDelegateReaction(
  delegating: Delegating<S, *, O>
): Single<Reaction<S, O>> = nextDelegateReactionCore(delegating).asSingleNeverOnCancel()

/**
 * This is a convenience method that wraps
 * [awaitWorkerResult][WorkflowPool.awaitWorkerResult] in a [Deferred] so it can
 * be selected on.
 *
 * @see WorkflowPool.awaitWorkerResult
 */
inline fun <reified I : Any, reified O : Any> WorkflowPool.workerResult(
  worker: Worker<I, O>,
  input: I,
  name: String = ""
): Single<O> = workerResultCore(worker, input, name).asSingleNeverOnCancel()

@PublishedApi
internal fun <T : Any> Deferred<T>.asSingleNeverOnCancel() = asSingle(Unconfined)
    .onErrorResumeNext {
      if (it is CancellationException) {
        Single.never()
      } else {
        Single.error(it)
      }
    }
