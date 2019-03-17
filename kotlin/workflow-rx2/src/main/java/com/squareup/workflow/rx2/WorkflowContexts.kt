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

import com.squareup.workflow.ChannelUpdate
import com.squareup.workflow.IdempotenceKey
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.onSuspending
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.openSubscription

/**
 * Subscribes to [single] and invokes [handler] with the value it emits.
 *
 * This allows us to use this method to, for example, wait for a response from a service call while
 * also moving between states in response to UI events. (Use [key] if you need to work simultaneously
 * with several `Single`s of the same type.)
 * The subscription will be disposed when `compose` returns without passing that single to this
 * method.
 */
inline fun <reified T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuccess(
  single: Single<out T>,
  key: String = "",
  noinline handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuccess(
    single,
    IdempotenceKey.fromGenericType(Single::class, T::class, key = key),
    handler
)

@PublishedApi
internal fun <T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuccess(
  single: Single<out T>,
  idempotenceKey: Any,
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuspending({ single.await() }, idempotenceKey, handler)

/**
 * Subscribes to [observable] and invokes [handler] when it emits the next value.
 *
 * This allows us to use this method to, for example, monitor a stream of values emitted by a single
 * hot observable over a series of state transitions. (Use [key] if you need to work simultaneously
 * with several `Observable`s of the same type.)
 * The subscription will be disposed when `compose` returns without passing that observable to this
 * method.
 */
inline fun <reified T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onNext(
  observable: Observable<out T>,
  key: String = "",
  noinline handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onNext(
    observable,
    IdempotenceKey.fromGenericType(Observable::class, T::class, key = key),
    handler
)

@PublishedApi
internal fun <T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onNext(
  observable: Observable<out T>,
  idempotenceKey: Any,
  handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onReceiveRaw({
  observable
      .doOnError {
        println("error: $it")
      }
      .openSubscription()
}, idempotenceKey, handler)
