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
package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import com.squareup.workflow.asWorker
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import org.reactivestreams.Publisher

/**
 * Creates a [Worker] from this [Observable].
 *
 * The [Observable] will be subscribed to when the [Worker] is started, and disposed when it is
 * cancelled.
 *
 * RxJava doesn't allow nulls, but it can't express that in its types. The receiver type parameter
 * is nullable so that the resulting [Worker] is non-nullable instead of having
 * platform nullability.
 */
inline fun <reified T : Any> Observable<out T?>.asWorker(): Worker<T> =
  this.toFlowable(BUFFER).asWorker()

/**
 * Creates a [Worker] from this [Publisher] ([Flowable] is a [Publisher]).
 *
 * The [Publisher] will be subscribed to when the [Worker] is started, and cancelled when it is
 * cancelled.
 *
 * RxJava doesn't allow nulls, but it can't express that in its types. The receiver type parameter
 * is nullable so that the resulting [Worker] is non-nullable instead of having
 * platform nullability.
 */
inline fun <reified T : Any> Publisher<out T?>.asWorker(): Worker<T> =
// This cast works because RxJava types don't actually allow nulls, it's just that they can't
  // express that in their types because Java.
  @Suppress("UNCHECKED_CAST")
  (this as Publisher<T>).asFlow().asWorker()

/**
 * Creates a [Worker] from this [Maybe].
 *
 * The [Maybe] will be subscribed to when the [Worker] is started, and disposed when it is
 * cancelled.
 *
 * RxJava doesn't allow nulls, but it can't express that in its types. The receiver type parameter
 * is nullable so that the resulting [Worker] is non-nullable instead of having
 * platform nullability.
 */
inline fun <reified T : Any> Maybe<out T?>.asWorker(): Worker<T> =
  Worker.fromNullable { await() }

/**
 * Creates a [Worker] from this [Single].
 *
 * The [Single] will be subscribed to when the [Worker] is started, and disposed when it is
 * cancelled.
 *
 * RxJava doesn't allow nulls, but it can't express that in its types. The receiver type parameter
 * is nullable so that the resulting [Worker] is non-nullable instead of having
 * platform nullability.
 */
inline fun <reified T : Any> Single<out T?>.asWorker(): Worker<T> =
// This !! works because RxJava types don't actually allow nulls, it's just that they can't
  // express that in their types because Java.
  Worker.from { await()!! }

/**
 * Creates a [Worker] from this [Completable].
 *
 * The [Completable] will be subscribed to when the [Worker] is started, and disposed when it is
 * cancelled.
 *
 * The key is required for this operator because there is no type information available to
 * distinguish workers.
 */
fun Completable.asWorker() = Worker.createSideEffect { await() }
