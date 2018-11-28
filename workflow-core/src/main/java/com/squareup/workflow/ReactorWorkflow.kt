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
 * When [react][Reactor.react] returns [FinishWith], a [CompletableDeferred] representing the
 * workflow result is completed, and then the broadcast channel is closed.
 *
 * When the workflow is cancelled, the reactor's [abandon][Reactor.abandon] is first called, and
 * then the coroutine running the broadcast loop is cancelled.
 *
 * _Note:_
 * If [Reactor.react] immediately returns [FinishWith], the last state may not be emitted since
 * the [state channel][openSubscriptionToState] is closed immediately.
 *
 * @param result Passed in so we can use implementation-by-delegation.
 */
internal class ReactorWorkflow<S : Any, in E : Any, out O : Any> private constructor(
  private val reactor: Reactor<S, E, O>,
  private val initialState: S,
  override val coroutineContext: CoroutineContext,
  private val result: CompletableDeferred<O>
) : Workflow<S, E, O>, Deferred<O> by result, CoroutineScope {

  constructor(
    reactor: Reactor<S, E, O>,
    initialState: S,
    context: CoroutineContext
  ) : this(
      reactor, initialState,
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
      // If the reason is null it means the result was completed normally, and we don't want
      // to abort the reactor in that case.
      if (cancelReason == null) return@invokeOnCompletion

      try {
        if (cancelReason is CancellationException) {
          // Only call abandon if we were actually cancelled – don't do it if the reactor throws.
          // Abandon may throw, and we don't want to lose the original exception.
          currentState?.let(reactor::abandon)
        }
      } catch (@Suppress("TooGenericExceptionCaught") abandonException: Throwable) {
        // The contract for invokeOnCompletion forbids us from throwing any exceptions ourselves,
        // and we can't just tack it onto the original exception because it will be ignored.
        handleUncaughtException(abandonException)
      } finally {
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
