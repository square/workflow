package com.squareup.workflow

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Models a process in the app as a stream of [states][openSubscriptionToState] of type [S],
 * followed by a [result][await] of type [O] when the process is done.
 *
 * A `Workflow` can be cancelled by calling [cancel].
 */
interface Workflow<out S : Any, in E : Any, out O : Any> : Deferred<O>, WorkflowInput<E> {
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
}

/**
 * [CoroutineContext] used by [Workflow] operators below.
 */
private val operatorContext: CoroutineContext = Unconfined

/**
 * [Transforms][https://stackoverflow.com/questions/15457015/explain-contramap]
 * the receiver to accept events of type [E2] instead of [E1].
 */
fun <S : Any, E2 : Any, E1 : Any, O : Any> Workflow<S, E1, O>.adaptEvents(transform: (E2) -> E1):
    Workflow<S, E2, O> = object : Workflow<S, E2, O>, Deferred<O> by this {
  override fun openSubscriptionToState(): ReceiveChannel<S> =
    this@adaptEvents.openSubscriptionToState()

  override fun sendEvent(event: E2) = this@adaptEvents.sendEvent(transform(event))
}

/**
 * Transforms the receiver to emit states of type [S2] instead of [S1].
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.mapState(
  transform: suspend (S1) -> S2
): Workflow<S2, E, O> = object : Workflow<S2, E, O>,
    Deferred<O> by this,
    WorkflowInput<E> by this {
  override fun openSubscriptionToState(): ReceiveChannel<S2> =
    GlobalScope.produce(operatorContext) {
      val source = this@mapState.openSubscriptionToState()
      source.consumeEach {
        send(transform(it))
      }
    }
}

/**
 * Like [mapState], transforms the receiving workflow with [Workflow.state] of type
 * [S1] to one with states of [S2]. Unlike that method, each [S1] update is transformed
 * into a stream of [S2] updates -- useful when an [S1] state might wrap an underlying
 * workflow whose own screens need to be shown.
 */
fun <S1 : Any, S2 : Any, E : Any, O : Any> Workflow<S1, E, O>.switchMapState(
  transform: suspend (S1) -> ReceiveChannel<S2>
): Workflow<S2, E, O> = object : Workflow<S2, E, O>,
    Deferred<O> by this,
    WorkflowInput<E> by this {
  override fun openSubscriptionToState(): ReceiveChannel<S2> =
    GlobalScope.produce(operatorContext) {
      val source = this@switchMapState.openSubscriptionToState()
      source.consumeEach { rawState ->
        transform(rawState).consumeEach { transformedState ->
          send(transformedState)
        }
      }
    }
}

/**
 * Transforms the receiver to emit a result of type [O2] instead of [O1].
 */
fun <S : Any, E : Any, O1 : Any, O2 : Any> Workflow<S, E, O1>.mapResult(
  transform: suspend (O1) -> O2
): Workflow<S, E, O2> {
  val transformedResult = GlobalScope.async(operatorContext) {
    transform(this@mapResult.await())
  }
  return object : Workflow<S, E, O2>,
      Deferred<O2> by transformedResult,
      WorkflowInput<E> by this {
    override fun openSubscriptionToState(): ReceiveChannel<S> =
      this@mapResult.openSubscriptionToState()
  }
}
