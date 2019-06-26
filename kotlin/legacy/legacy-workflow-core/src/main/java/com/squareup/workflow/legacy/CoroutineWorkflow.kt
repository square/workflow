/*
 * Copyright 2018 Square Inc.
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

package com.squareup.workflow.legacy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Starts a coroutine in the given [context] that runs [block] to produce states and the result
 * for the returned [Workflow].
 *
 * The block gets a [SendChannel] that it can use to emit [states][S], and a [ReceiveChannel] that
 * it can use to receive [events][E] from the workflow's [WorkflowInput]. The block's return value
 * is used as the workflow's result.
 * The state channel is a
 * [ConflatedBroadcastChannel][kotlinx.coroutines.channels.ConflatedBroadcastChannel],
 * which means that sends will never suspend or fail, and if observers of the workflow aren't
 * receiving fast enough, they will miss intermediate states.
 * The event channel has [unlimited capacity][UNLIMITED].
 */
@Suppress("DEPRECATION")
@Deprecated("Use com.squareup.workflow.Workflow")
fun <S : Any, E : Any, O : Any> CoroutineScope.workflow(
  context: CoroutineContext = EmptyCoroutineContext,
  block: suspend CoroutineScope.(
    state: SendChannel<S>,
    events: ReceiveChannel<E>
  ) -> O
): Workflow<S, E, O> {
  // The events channel is buffered primarily so that reactors don't race themselves when state
  // changes immediately cause more events to be sent.
  val events = Channel<E>(UNLIMITED)
  val state = BroadcastChannel<S>(CONFLATED)
  val result = async(context) {
    block(state, events)
  }

  result.invokeOnCompletion { cancelReason ->
    state.close(cancelReason)
    // Don't hold onto any remaining queued events, since they will never be read.
    events.close(cancelReason)
  }

  return object : Workflow<S, E, O>, Deferred<O> by result {
    override fun openSubscriptionToState(): ReceiveChannel<S> = state.openSubscription()

    override fun sendEvent(event: E) {
      // The events channel has an unlimited-size buffer, this will only fail
      // if the event channel was closed.
      try {
        events.offer(event)
      } catch (e: CancellationException) {
        // This means that the workflow was cancelled. Senders shouldn't care if the workflow
        // accepted the event or not.
      } catch (e: ClosedSendChannelException) {
        // This may mean the workflow finished or that the workflow closed the events channel
        // itself. Senders shouldn't care if the workflow accepted the event or not.
      }
    }

    override fun toString(): String = "workflow($context)"
  }
}
