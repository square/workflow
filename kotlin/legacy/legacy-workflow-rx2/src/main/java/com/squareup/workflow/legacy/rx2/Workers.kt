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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy.rx2

import com.squareup.workflow.legacy.Worker
import com.squareup.workflow.legacy.worker
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

/**
 * Creates a [Worker] that will pass its input value to [block], then subscribe to the returned
 * [Single] and report the value it emits as the worker result.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
fun <I : Any, O : Any> singleWorker(block: (I) -> Single<O>): Worker<I, O> =
  worker { block(it).await() }

/**
 * Creates a [Worker] that will report the [Single]'s eventual value as its result.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
fun <T : Any> Single<T>.asWorker(): Worker<Unit, T> = worker { await() }
