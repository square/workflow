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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType

/**
 * Facilities for a [Workflow] to interact with other [Workflow]s and the outside world from inside
 * a `compose` function.
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
 *
 * ## Handling Workflow Teardown
 *
 * See [onTeardown].
 */
abstract class WorkflowContext<StateT : Any, in OutputT : Any> {

  /**
   * Given a function that takes an [event][EventT] and can mutate the state or emit an output,
   * returns a function that will perform that workflow update when called with an event.
   * The returned function is valid until the next compose pass.
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
  abstract fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): EventHandler<EventT>

  /**
   * Ensures [child] is running as a child of this workflow, and returns the result of its
   * `compose` method.
   *
   * **Never call [StatefulWorkflow.compose] or [StatelessWorkflow.compose] directly, always do it
   * through this context method.**
   *
   * 1. If the child _wasn't_ already running, it will be started either from
   *    [initialState][Workflow.initialState] or its snapshot.
   * 2. If the child _was_ already running, The workflow's
   *    [onInputChanged][StatefulWorkflow.onInputChanged] method is invoked with the previous input
   *    and this one.
   * 3. The child's `compose` method is invoked with `input` and the child's state.
   *
   * After this method returns, if something happens that trigger's one of `child`'s handlers, and
   * that handler emits an output, the function passed as [handler] will be invoked with that
   * output.
   *
   * @param key An optional string key that is used to distinguish between workflows of the same
   * type.
   */
  abstract fun <ChildInputT : Any, ChildOutputT : Any, ChildRenderingT : Any> compose(
    child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    input: ChildInputT,
    key: String = "",
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT

  /**
   * Adds an action to be invoked if the workflow is discarded by its parent before the next
   * compose pass. Multiple hooks can be registered in the same compose pass, they will be invoked
   * in the same order they're set. Like any other work performed through the context (e.g. calls
   * to [compose] or [onReceive]), hooks are cleared at the start of each compose pass, so you must
   * set any hooks you need in each compose pass.
   *
   * Teardown handlers should be non-blocking and execute quickly, since they are invoked
   * synchronously during the compose pass.
   */
  abstract fun onTeardown(handler: () -> Unit)

  /**
   * Implementation of [WorkflowContext.Companion.createSubscription].
   */
  protected abstract fun <E> createSubscription(
    emitterType: KClass<*>,
    emissionType: KClass<*>,
    key: String,
    handler: (ChannelUpdate<E>) -> WorkflowAction<StateT, OutputT>,
    subscribe: CoroutineScope.() -> ReceiveChannel<E>
  )

  companion object {

    /**
     * TODO kdoc
     * Not intended to be used by regular application code. Defined as a companion object function
     * instead of a method with a [WorkflowContext] receiver so it doesn't show up in autocomplete.
     */
    fun <StateT : Any, OutputT : Any, E> createSubscription(
      context: WorkflowContext<StateT, OutputT>,
      emitterType: KClass<*>,
      emissionType: KClass<*>,
      key: String,
      handler: (ChannelUpdate<E>) -> WorkflowAction<StateT, OutputT>,
      subscribe: CoroutineScope.() -> ReceiveChannel<E>
    ) = context.createSubscription(emitterType, emissionType, key, handler, subscribe)
  }
}

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input.
 */
fun <StateT : Any, OutputT : Any, ChildOutputT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, ChildOutputT, ChildRenderingT>,
      key: String = "",
      handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
    ): ChildRenderingT = compose(child, Unit, key, handler)
// @formatter:on

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input or emit
 * output.
 */
fun <InputT : Any, StateT : Any, OutputT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<InputT, Nothing, ChildRenderingT>,
      input: InputT,
      key: String = ""
    ): ChildRenderingT = compose(child, input, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Convenience alias of [WorkflowContext.compose] for workflows that don't take input or emit
 * output.
 */
fun <StateT : Any, OutputT : Any, ChildRenderingT : Any>
    WorkflowContext<StateT, OutputT>.compose(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, Nothing, ChildRenderingT>,
      key: String = ""
    ): ChildRenderingT = compose(child, Unit, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Will send anything emitted on the [channel] to [handler] as a [ChannelUpdate].
 *
 * This method ensures that only one subscription is active at a time for the given key.
 * If this method is called in two or more consecutive `compose` invocations with the same
 * key+[channel] type, the provider will only be invoked for the first one, and the returned
 * channel will be re-used for all subsequent invocations, until a `compose` invocation
 * does _not_ call this method with an equal key+type. At that time, the channel will be
 * [cancelled][ReceiveChannel.cancel], with the assumption that cancelling the channel will release
 * any resources associated with the [channel].
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * type.
 * @param handler A function that returns the [WorkflowAction] to perform when the channel emits.
 */
inline fun <StateT : Any, OutputT : Any, reified T> WorkflowContext<StateT, OutputT>.onReceive(
  channel: ReceiveChannel<T>,
  key: String = "",
  noinline handler: (ChannelUpdate<T>) -> WorkflowAction<StateT, OutputT>
) {
  WorkflowContext.createSubscription(
      context = this,
      emitterType = ReceiveChannel::class,
      emissionType = T::class,
      key = key,
      handler = handler,
      subscribe = { channel }
  )
}

/**
 * Will wait for [deferred] to complete, then pass its value to [handler]. Once the handler has been
 * invoked for a given deferred+key, it will not be invoked again until an invocation of
 * `compose` that does _not_ call this method with that deferred+[key].
 *
 * @param key An optional string key that is used to distinguish between subscriptions of the same
 * type.
 */
inline fun <reified T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onDeferred(
  deferred: Deferred<T>,
  key: String = "",
  noinline handler: (T) -> WorkflowAction<StateT, OutputT>
) = onSuspending({ deferred.await() }, key, handler)

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
inline fun <reified T, StateT : Any, OutputT : Any> WorkflowContext<StateT, OutputT>.onSuspending(
  noinline function: suspend () -> T,
  key: String = "",
  crossinline handler: (T) -> WorkflowAction<StateT, OutputT>
) {
  WorkflowContext.createSubscription(
      context = this,
      emitterType = KFunction::class,
      emissionType = T::class,
      key = key,
      handler = { update ->
        if (update !is Value) {
          throw AssertionError("Suspend function channel should never close.")
        }
        handler(update.value)
      },
      subscribe = { wrapInNeverClosingChannel(function) }
  )
}

/**
 * Invokes [function] and returns a channel that will emit the return value of the function when it
 * returns, and then will never close.
 */
@PublishedApi internal fun <T> CoroutineScope.wrapInNeverClosingChannel(
  function: suspend () -> T
): ReceiveChannel<T> =
  produce {
    send(function())
    // We explicitly don't want to close the channel, because that would trigger an infinite loop.
    // Instead, just suspend forever.
    suspendCancellableCoroutine<Nothing> { }
  }
