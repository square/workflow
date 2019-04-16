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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.rx2

import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.onSuspending
import com.squareup.workflow.util.ChannelUpdate
import com.squareup.workflow.util.KTypes
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.openSubscription
import kotlin.reflect.KType

/**
 * Subscribes to [single] and invokes [handler] with the value it emits.
 *
 * This allows us to use this method to, for example, wait for a response from a service call while
 * also moving between states in response to UI events. (Use [key] if you need to work simultaneously
 * with several `Single`s of the same type.)
 * The subscription will be disposed when `render` returns without passing a single of the same
 * type and with the same key to this method.
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * type.
 */
inline fun <reified T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuccess(
  single: Single<out T>,
  key: String = "",
  noinline handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuccess(
    single,
    KTypes.fromGenericType(Single::class, T::class),
    key,
    handler
)

/**
 * Subscribes to [single] and invokes [handler] with the value it emits.
 *
 * This allows us to use this method to, for example, wait for a response from a service call while
 * also moving between states in response to UI events. (Use [key] if you need to work simultaneously
 * with several `Single`s of the same type.)
 * The subscription will be disposed when `render` returns without passing a single of the same
 * type and with the same key to this method.
 *
 * @param type The [KType] that represents both the type of data source (e.g. `ReceiveChannel` or
 * `Observable`) and the element type [T].
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * [type].
 */
fun <T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuccess(
  single: Single<out T>,
  type: KType,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuspending({ single.await() }, type, key, handler)

/**
 * Subscribes to [observable] and invokes [handler] when it emits the next value.
 *
 * This allows us to use this method to, for example, monitor a stream of values emitted by a single
 * hot observable over a series of state transitions. (Use [key] if you need to work simultaneously
 * with several `Observable`s of the same type.)
 * The subscription will be disposed when `render` returns without passing an observable of the
 * same type and with the same key to this method.
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * type.
 */
inline fun <reified T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onNext(
  observable: Observable<out T>,
  key: String = "",
  noinline handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onNext(
    observable,
    KTypes.fromGenericType(Observable::class, T::class),
    key,
    handler
)

/**
 * Subscribes to [observable] and invokes [handler] when it emits the next value.
 *
 * This allows us to use this method to, for example, monitor a stream of values emitted by a single
 * hot observable over a series of state transitions. (Use [key] if you need to work simultaneously
 * with several `Observable`s of the same type.)
 * The subscription will be disposed when `render` returns without passing an observable of the
 * same type and with the same key to this method.
 *
 * @param type The [KType] that represents both the type of data source (e.g. `ReceiveChannel` or
 * `Observable`) and the element type [T].
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * [type].
 */
fun <T : Any, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onNext(
  observable: Observable<out T>,
  type: KType,
  key: String = "",
  handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onReceive({ observable.openSubscription() }, type, key, handler)
