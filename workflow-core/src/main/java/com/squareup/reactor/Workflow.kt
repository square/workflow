package com.squareup.reactor

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Models a process in the app as a stream of [states][onState] of type [S], followed by
 * a [result][awaitResult] of type [O] when the process is done.
 */
interface Workflow<out S : Any, in E : Any, out O : Any> : WorkflowInput<E> {
  /**
   * Returns a channel that will, on every update, report the complete, current state of this
   * workflow.
   *
   * The channel must be canceled to unsubscribe from the stream.
   *
   * Note that, unlike RxJava-style `Observable`s, the returned [ReceiveChannel]s are not
   * multicasting – multiple consumers reading from the same channel will only see distinct values.
   * Each consumer must get its own channel by calling this method.
   *
   * The channel will throw a [CancellationException] if the workflow is abandoned. This method
   * itself will not throw.
   */
  fun openSubscriptionToState(): ReceiveChannel<S>

  /**
   * Returns the final result of this workflow. The result value should be cached – that is,
   * the result should be stored and emitted even if not called until after the workflow completes.
   *
   * @throws CancellationException If the workflow is abandoned while waiting for a result or
   * before.
   */
  suspend fun awaitResult(): O

  /**
   * May be called to advise a running workflow that it is to be abandoned prematurely, before
   * it has fired its [result][awaitResult]. Provided as an optional hook to allow implementations
   * to handle any tear-down concerns.
   *
   * After this method is called, all channels returned from [openSubscriptionToState], as well as
   * any suspended [awaitResult] calls, will throw [CancellationException].
   */
  fun abandon()
}

/**
 * [Transforms][https://stackoverflow.com/questions/15457015/explain-contramap]
 * the receiver to accept events of type [E2] instead of [E1].
 */
fun <S : Any, E2 : Any, E1 : Any, O : Any> Workflow<S, E1, O>.adaptEvents(transform: (E2) -> E1):
    Workflow<S, E2, O> {
  return object : Workflow<S, E2, O> {
    override fun openSubscriptionToState(): ReceiveChannel<S> =
      this@adaptEvents.openSubscriptionToState()

    override suspend fun awaitResult(): O = this@adaptEvents.awaitResult()
    override fun sendEvent(event: E2) = this@adaptEvents.sendEvent(transform(event))
    override fun abandon() = this@adaptEvents.abandon()
  }
}

/**
 * Transforms the receiver to emit states of type [S2] instead of [S1].
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.mapState(
  transform: suspend (S1) -> S2,
  context: CoroutineContext = Dispatchers.Unconfined
): Workflow<S2, E, O> = object : CoroutineScopedWorkflow<S2, E, O>(context) {
  override fun openSubscriptionToState(): ReceiveChannel<S2> = produce {
    this@mapState.openSubscriptionToState()
        .consumeEach {
          send(transform(it))
        }
  }

  override suspend fun awaitResult(): O = this@mapState.awaitResult()
  override fun sendEvent(event: E) = this@mapState.sendEvent(event)
  override fun abandon() = this@mapState.abandon()
}

/**
 * Like [mapState], transforms the receiving workflow with [Workflow.state] of type
 * [S1] to one with states of [S2]. Unlike that method, each [S1] update is transformed
 * into a stream of [S2] updates -- useful when an [S1] state might wrap an underlying
 * workflow whose own screens need to be shown.
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.switchMapState(
  transform: suspend (S1) -> ReceiveChannel<S2>,
  context: CoroutineContext = Dispatchers.Unconfined
): Workflow<S2, E, O> = object : CoroutineScopedWorkflow<S2, E, O>(context) {
  override fun openSubscriptionToState(): ReceiveChannel<S2> = produce {
    this@switchMapState.openSubscriptionToState()
        .consumeEach { rawState ->
          transform(rawState).consumeEach { transformedState ->
            send(transformedState)
          }
        }
  }

  override suspend fun awaitResult(): O = this@switchMapState.awaitResult()
  override fun sendEvent(event: E) = this@switchMapState.sendEvent(event)
  override fun abandon() = this@switchMapState.abandon()
}

/**
 * Transforms the receiver to emit a result of type [O2] instead of [O1].
 */
fun <S : Any, E : Any, O1 : Any, O2 : Any> Workflow<S, E, O1>.mapResult(
  transform: suspend (O1) -> O2
): Workflow<S, E, O2> {
  return object : Workflow<S, E, O2> {
    override fun openSubscriptionToState(): ReceiveChannel<S> =
      this@mapResult.openSubscriptionToState()

    override suspend fun awaitResult(): O2 = transform(this@mapResult.awaitResult())
    override fun sendEvent(event: E) = this@mapResult.sendEvent(event)
    override fun abandon() = this@mapResult.abandon()
  }
}
