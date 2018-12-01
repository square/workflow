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
package com.squareup.workflow

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.broadcast
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Implements a [Workflow] using a [Reactor].
 *
 * The react loop runs inside a coroutine. For each state, the state value is sent on a
 * [ConflatedBroadcastChannel][kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel].
 * When [react][Reactor.onReact] returns [FinishWith], a [CompletableDeferred] representing the
 * workflow result is completed, and then the broadcast channel is closed.
 *
 * _Note:_
 * If [Reactor.onReact] immediately returns [FinishWith], the last state may not be emitted since
 * the [state channel][openSubscriptionToState] is closed immediately.
 *
 * @param result Passed in so we can use implementation-by-delegation.
 */
internal class ReactorWorkflow<S : Any, in E : Any, out O : Any> private constructor(
  private val reactor: Reactor<S, E, O>,
  private val initialState: S,
  private val workflows: WorkflowPool,
  override val coroutineContext: CoroutineContext,
  private val result: CompletableDeferred<O>
) : Workflow<S, E, O>, Deferred<O> by result, CoroutineScope {

  constructor(
    reactor: Reactor<S, E, O>,
    initialState: S,
    workflows: WorkflowPool,
    context: CoroutineContext
  ) : this(
      reactor,
      initialState,
      workflows,
      // Unconfined means the coroutine is never dispatched and always resumed synchronously on the
      // current thread. This is the default RxJava behavior, and the behavior we explicitly want
      // here.
      Unconfined + context,
      CompletableDeferred()
  )

  // The events channel is buffered primarily so that reactors don't race themselves when state
  // changes immediately cause more events to be sent.
  private val events = Channel<E>(UNLIMITED)

  private var currentState: S? = null

  override val isActive: Boolean get() = result.isActive

  override fun sendEvent(event: E) {
    // The events channel has an unlimited-size buffer, this can never fail.
    events.offer(event)
  }

  /**
   * Broadcast coroutines start as [LAZY][CoroutineStart.LAZY] by default. We override that to
   * start as [UNDISPATCHED] so that the workflow runs even if the state isn't subscribed to.
   */
  private val stateBroadcast = broadcast(
      capacity = CONFLATED,
      start = UNDISPATCHED
  ) {
    var reaction: Reaction<S, O> = EnterState(initialState)

    try {
      while (reaction is EnterState) {
        val state = reaction.state
        currentState = state
        send(state)
        reaction = tryReact(state)
      }
      result.complete((reaction as FinishWith).result)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
      // Cleanup if we didn't complete normally.
      // See comment in init below for why we're not relying on structured concurrency.
      result.cancel(e)
      throw e
    }
  }

  init {
    result.invokeOnCompletion { cancelReason ->
      // If the reason is null it means the result was completed normally, and we don't need to
      // do anything special.
      if (cancelReason != null) {
        // We explicitly cancel the broadcast coroutine instead of making the result the parent
        // job because we need to ensure that the reactor is abandoned before the state channel
        // completes.
        //
        // If the workflow finished normally, we want to just let the channel close naturally.
        // We only want to cancel the coroutine if the workflow was cancelled.
        // If the result is completing because the broadcast coroutine failed, this will be a noop.
        stateBroadcast.cancel(cancelReason)
      }
    }
  }

  override fun openSubscriptionToState(): ReceiveChannel<S> = stateBroadcast.openSubscription()

  override fun toString(): String {
    return "${this::class.simpleName}($reactor @ $currentState)"
  }

  /**
   * Ensures any errors thrown either by the [Reactor.onReact] method or the [Single] it returns are
   * wrapped in a [ReactorException] and transmitted through Rx plumbing.
   */
  @Suppress("TooGenericExceptionCaught")
  private suspend fun tryReact(currentState: S): Reaction<S, O> =
    try {
      reactor.onReact(currentState, events, workflows)
    } catch (cause: Throwable) {
      // CancellationExceptions aren't actually errors, rethrow them directly.
      if (cause is CancellationException) throw cause
      throw ReactorException(
          cause = cause,
          reactor = reactor,
          reactorState = currentState
      )
    }
}

/**
 * Handles the uncaught exception using the [CoroutineContext]'s [CoroutineExceptionHandler] if one
 * exists, else the [Thread.uncaughtExceptionHandler].
 */
internal fun CoroutineScope.handleUncaughtException(e: Throwable) {
  coroutineContext[CoroutineExceptionHandler]?.handleException(coroutineContext, e)
      ?: Thread.currentThread().let { thread ->
        thread.uncaughtExceptionHandler.uncaughtException(thread, e)
      }
}
