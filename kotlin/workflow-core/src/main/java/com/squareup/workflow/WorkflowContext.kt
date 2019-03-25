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

package com.squareup.workflow

import com.squareup.workflow.util.ChannelUpdate
import com.squareup.workflow.util.ChannelUpdate.Value
import com.squareup.workflow.util.KTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KType

/**
 * Facilities for a [Workflow] to interact with other [Workflow]s and the outside world from inside
 * a [compose][Workflow.compose] function.
 *
 * ## Handling Events
 *
 * See [onEvent].
 *
 * ## Handling External Reactive Streams
 *
 * See [onReceive], [onDeferred], and [onSuspending].
 *
 * ## Composing Children
 *
 * See [compose].
 */
interface WorkflowContext<StateT : Any, in OutputT : Any> {

  /**
   * Given a function that takes an [event][EventT] and can mutate the state or emit an output, returns
   * a function that will perform that workflow update when called with an event.
   * The returned function is valid until the next [compose][Workflow.compose] pass.
   *
   * For example, if you have a rendering type of `Screen`:
   *
   *    data class Screen(
   *      val label: String,
   *      val onClick: () -> Unit
   *    )
   *
   * Then, from your `compose` method, construct the screen like this:
   *
   *    return Screen(
   *      button1Label = "Hello",
   *      button2Label = "World",
   *      onClick = context.onEvent { buttonIndex ->
   *        emitOutput("Button $buttonIndex clicked!")
   *      }
   *    )
   *
   * @param handler A function that returns the [WorkflowAction] to perform when the event handler
   * is invoked.
   */
  fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): EventHandler<EventT>

  /**
   * Ensures that the [channel][ReceiveChannel] returned from [channelProvider] is subscribed to, and
   * will send anything emitted on the channel to [handler] as a [ChannelUpdate].
   *
   * This method ensures that only one subscription is active at a time for the given [type]+[key].
   * If this method is called in two or more consecutive [Workflow.compose] invocations with the same
   * key, [channelProvider] will only be invoked for the first one, and the returned channel will be
   * re-used for all subsequent invocations, until a [Workflow.compose] invocation does _not_ call this
   * method with an equal key. At that time, the channel will be [cancelled][ReceiveChannel.cancel],
   * with the assumption that cancelling the channel will release any resources allocated by the
   * [channelProvider].
   *
   * @param type The [KType] that represents both the type of data source (e.g. `ReceiveChannel` or
   * `Deferred`) and the element type [E].
   * @param key An optional string key that is used to distinguish between subscriptions of the same
   * [type].
   * @param handler A function that returns the [WorkflowAction] to perform when the channel emits.
   *
   * @see onReceive
   */
  fun <E> onReceive(
    channelProvider: CoroutineScope.() -> ReceiveChannel<E>,
    type: KType,
    key: String = "",
    handler: (ChannelUpdate<E>) -> WorkflowAction<StateT, OutputT>
  )

  /**
   * Ensures [child] is running as a child of this workflow, and returns the result of its
   * [compose][Workflow.compose] method.
   *
   * **Never call [Workflow.compose] directly, always do it through this context method.**
   *
   * 1. If the child _wasn't_ already running, it will be started either from
   *    [initialState][Workflow.initialState] or its snapshot.
   * 2. If the child _was_ already running, The workflow's [onInputChanged][Workflow.onInputChanged]
   *    method is invoked with the previous input and this one.
   * 3. The child's [compose][Workflow.compose] method is invoked with `input` and the child's state.
   *
   * After this method returns, if something happens that trigger's one of `child`'s handlers, and
   * that handler emits an output, the function passed as [handler] will be invoked with that output.
   *
   * @param key An optional string key that is used to distinguish between workflows of the same
   * type.
   */
  fun <ChildInputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any> compose(
    child: Workflow<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
    input: ChildInputT,
    key: String = "",
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT
}

/**
 * Ensures that the [channel][ReceiveChannel] returned from [channelProvider] is subscribed to, and
 * will send anything emitted on the channel to [handler] as a [ChannelUpdate].
 *
 * This method ensures that only one subscription is active at a time for the given key.
 * If this method is called in two or more consecutive [Workflow.compose] invocations with the same
 * key+[channelProvider] type, the provider will only be invoked for the first one, and the returned
 * channel will be re-used for all subsequent invocations, until a [Workflow.compose] invocation does
 * _not_ call this method with an equal key+type. At that time, the channel will be
 * [cancelled][ReceiveChannel.cancel], with the assumption that cancelling the channel will release
 * any resources allocated by the [channelProvider].
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same type.
 * @param handler A function that returns the [WorkflowAction] to perform when the channel emits.
 *
 * @see WorkflowContext.onReceive
 */
inline fun <StateT : Any, OutputT : Any, reified T> WorkflowContext<StateT, OutputT>.onReceive(
  noinline channelProvider: CoroutineScope.() -> ReceiveChannel<T>,
  key: String = "",
  noinline handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onReceive(
    channelProvider,
    KTypes.fromGenericType(ReceiveChannel::class, T::class),
    key,
    handler
)

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input.
 */
fun <StateT : Any, OutputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, ChildStateT, ChildOutputT, ChildRenderingT>,
      key: String = "",
      handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
    ): ChildRenderingT = compose(child, Unit, key, handler)
// @formatter:on

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input or emit output.
 */
fun <InputT : Any, StateT : Any, OutputT : Any, ChildStateT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<InputT, ChildStateT, Nothing, ChildRenderingT>,
      input: InputT,
      key: String = ""
    ): ChildRenderingT = compose(child, input, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input or emit output.
 */
fun <StateT : Any, OutputT : Any, ChildStateT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, ChildStateT, Nothing, ChildRenderingT>,
      key: String = ""
    ): ChildRenderingT = compose(child, Unit, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Will wait for [deferred] to complete, then pass its value to [handler]. Once the handler has been
 * invoked for a given deferred+key, it will not be invoked again until an invocation of
 * [Workflow.compose] that does _not_ call this method with that deferred+[key].
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * type.
 */
inline fun <reified T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onDeferred(
  deferred: Deferred<T>,
  key: String = "",
  noinline handler: (T) -> WorkflowAction<StateT, OutputT>
) = onDeferred(
    deferred,
    KTypes.fromGenericType(Deferred::class, T::class),
    key,
    handler
)

/**
 * Will wait for [deferred] to complete, then pass its value to [handler]. Once the handler has been
 * invoked for a given deferred+key, it will not be invoked again until an invocation of
 * [Workflow.compose] that does _not_ call this method with that deferred+[key].
 *
 * @param type The [KType] that represents both the type of data source (e.g. `Deferred`) and the
 * element type [T].
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * [type].
 */
fun <T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onDeferred(
  deferred: Deferred<T>,
  type: KType,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuspending({ deferred.await() }, type, key, handler)

/**
 * This function is provided as a helper for writing [WorkflowContext] extension functions, it
 * should not be used by general application code.
 *
 * @param type The [KType] that represents both the type of data source (e.g. `Deferred`) and the
 * element type [T].
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * [type].
 * @param function A suspending function that is invoked in a coroutine that will be a child of this
 * state machine. The function will be cancelled if the state machine is cancelled.
 */
fun <T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuspending(
  function: suspend () -> T,
  type: KType,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onReceive(
    { wrapInNeverClosingChannel(function) },
    type,
    key,
    { update ->
      if (update !is Value) {
        throw AssertionError("Suspend function channel should never close.")
      }
      return@onReceive handler(update.value)
    }
)

/**
 * Invokes [function] and returns a channel that will emit the return value of the function when it
 * returns, and then will never close.
 */
private fun <T> CoroutineScope.wrapInNeverClosingChannel(
  function: suspend () -> T
): ReceiveChannel<T> =
  produce {
    send(function())
    // We explicitly don't want to close the channel, because that would trigger an infinite loop.
    // Instead, just suspend forever.
    suspendCancellableCoroutine<Nothing> { }
  }
