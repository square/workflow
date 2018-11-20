package com.squareup.reactor

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.broadcast
import kotlin.coroutines.experimental.CoroutineContext

internal class ReactorWorkflow<S : Any, in E : Any, out O : Any>(
  private val reactor: Reactor<S, E, O>,
  private val initialState: S
) : Workflow<S, E, O>, CoroutineScope {

  // The events channel is buffered primarily so that reactors don't race themselves when state
  // changes immediately cause more events to be sent.
  private val events = Channel<E>(UNLIMITED)

  private val job = Job()

  private var currentState: S? = null

  /**
   * Necessary because there's a bug in pre-1.3 coroutines.
   * See https://github.com/square/workflow/issues/15.
   *
   * Once we're on 1.3, this should be replaced with a [stateBroadcast] that emits [Reaction]s.
   */
  private val result = CompletableDeferred<O>()

  // Unconfined means the coroutine is never dispatched and always resumed synchronously on the
  // current thread. This is the default RxJava behavior, and the behavior we explicitly want here.
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined + job

  // The events channel has an unlimited-size buffer, this can never fail.
  override fun sendEvent(event: E) {
    events.offer(event)
  }

  private val stateBroadcast = broadcast(capacity = CONFLATED, start = UNDISPATCHED) {
    var reaction: Reaction<S, O> = EnterState(initialState)

    while (reaction is EnterState) {
      val state = reaction.state
      currentState = state
      send(state)
      reaction = tryReact(state)
    }
    result.complete((reaction as FinishWith).result)
  }.apply {
    invokeOnClose {
      // Cleanup if we didn't complete normally.
      if (it != null) result.cancel(it)
    }
  }

  override fun openSubscriptionToState(): ReceiveChannel<S> = stateBroadcast.openSubscription()

  override suspend fun awaitResult(): O = result.await()

  override fun abandon() {
    if (stateBroadcast.isClosedForSend) return
    // Docs promise that reactor gets told about the abandon before it's signaled through the
    // outputs. We have to explicitly abandon it before canceling the broadcast channel because
    // the channel will send the close signal before invoking any close handlers or resuming
    // itself.
    currentState?.let(reactor::abandon)
    stateBroadcast.cancel()
    events.close()
  }

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
      reactor.react(currentState, events)
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
