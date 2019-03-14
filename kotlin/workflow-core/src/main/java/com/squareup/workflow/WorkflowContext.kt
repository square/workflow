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
package com.squareup.workflow

import com.squareup.workflow.ChannelUpdate.Value
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

/**
 * Facilities for a [Workflow] to interact with other [Workflow]s and the outside world from inside
 * a [compose][Workflow.compose] function.
 *
 * ## Handling Events
 *
 * See [makeSink], [makeUnitSink].
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
   * If you don't need to pass event data in, you can just use `Unit` as your event type, or use
   * [makeUnitSink] to get a function that doesn't have any parameters.
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
   *      onClick = context.makeSink { buttonIndex ->
   *        emitOutput("Button $buttonIndex clicked!")
   *      }
   *    )
   *
   * @param handler A function that returns the [WorkflowAction] to perform when the event handler
   * is invoked.
   * @see makeUnitSink
   */
  fun <EventT : Any> makeSink(handler: (EventT) -> WorkflowAction<StateT, OutputT>): (EventT) -> Unit

  /**
   * Ensures that the [channel][ReceiveChannel] returned from [channelProvider] is subscribed to, and
   * will send anything emitted on the channel to [handler] as a [ChannelUpdate].
   *
   * This method ensures that only one subscription is active at a time for the given idempotence key,
   * where idempotence keys are compared using [equals].
   * If this method is called in two or more consecutive [Workflow.compose] invocations with the same
   * key, [channelProvider] will only be invoked for the first one, and the returned channel will be
   * re-used for all subsequent invocations, until a [Workflow.compose] invocation does _not_ call this
   * method with an equal key. At that time, the channel will be [cancelled][ReceiveChannel.cancel],
   * with the assumption that cancelling the channel will release any resources allocated by the
   * [channelProvider].
   *
   * **This method is intended for library authors**, since a unique idempotence key must be specified
   * explicitly. Most code will want to call one of the variants that accepts a [String] `key`
   * parameter (i.e. [onReceive]). See [IdempotenceKey] for factory methods to derive keys from types.
   *
   * @param handler A function that returns the [WorkflowAction] to perform when the channel emits.
   *
   * @see onReceive
   */
  fun <E> onReceiveRaw(
    channelProvider: CoroutineScope.() -> ReceiveChannel<E>,
    idempotenceKey: Any,
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
   */
  fun <ChildInputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any> compose(
    child: Workflow<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
    input: ChildInputT,
    key: String = "",
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT
}

/**
 * Returns a function that will invoke [handler] when called.
 *
 * This is a convenience function that is equivalent to calling [makeSink][WorkflowContext.makeSink]
 * and then passing `Unit` to the resulting sink.
 *
 * @see WorkflowContext.makeSink
 */
fun <StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.makeUnitSink(
  handler: () -> WorkflowAction<StateT, OutputT>
): () -> Unit {
  val sink = makeSink<Unit> { handler() }
  return { sink(Unit) }
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
 * @param handler A function that returns the [WorkflowAction] to perform when the channel emits.
 *
 * @see WorkflowContext.onReceiveRaw
 */
inline fun <StateT : Any, OutputT : Any, reified T> WorkflowContext<StateT, OutputT>.onReceive(
  noinline channelProvider: CoroutineScope.() -> ReceiveChannel<T>,
  key: String = "",
  noinline handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) = onReceiveRaw(
    channelProvider,
    IdempotenceKey.fromGenericType(ReceiveChannel::class, T::class, key = key),
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
 * [Workflow.compose] that does _not_ call this method with that deferred+key.
 */
inline fun <reified T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onDeferred(
  deferred: Deferred<T>,
  key: String = "",
  noinline handler: (T) -> WorkflowAction<StateT, OutputT>
) = onDeferred(
    deferred,
    IdempotenceKey.fromGenericType(Deferred::class, T::class, key = key),
    handler
)

@PublishedApi
internal fun <T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onDeferred(
  deferred: Deferred<T>,
  idempotenceKey: Any,
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuspending({ deferred.await() }, idempotenceKey, handler)

/**
 * This function is provided as a helper for writing [WorkflowContext] extension functions, it
 * should not be used by general application code.
 *
 * @param function A suspending function that is invoked in a coroutine that will be a child of this
 * state machine. The function will be cancelled if the state machine is cancelled.
 */
fun <T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuspending(
  function: suspend () -> T,
  idempotenceKey: Any,
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onReceiveRaw(
    { wrapInNeverClosingChannel(function) },
    idempotenceKey,
    { update ->
      if (update !is Value) {
        throw AssertionError("Suspend function channel should never close.")
      }
      return@onReceiveRaw handler(update.value)
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
