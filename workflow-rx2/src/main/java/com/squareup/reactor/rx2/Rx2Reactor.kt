package com.squareup.reactor.rx2

import com.squareup.reactor.CoroutineReactor
import com.squareup.reactor.Reaction
import com.squareup.workflow.Rx2Workflow
import io.reactivex.Single
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.rx2.await

/**
 * Implements a state machine where the states are represented by objects of type (or subclasses of
 * type) `S`.
 *
 * When the state machine is executed, each consecutive state will be passed to [onReact], which
 * returns a [Single] that eventually emits a [Reaction] indicating what to do next:
 *   * `EnterState(nextState)`: Call [onReact] again with `nextState`.
 *   * `FinishWith(result)`: The state machine is in a terminal state and has a result of type `O`.
 *
 * To use a [Rx2Reactor], call [Rx2Reactor.startWorkflow].
 *
 * For more information, including a how-to guide for writing Reactors, please see the `README.md`
 * file in this package.
 *
 * @param S State type
 * @param E Event type. If your Reactor doesn't have any events, use `Nothing`
 * @param O Output (result) type
 */
@Deprecated("Use Rx2ComposedReactor instead.")
interface Rx2Reactor<S : Any, E : Any, out O : Any> {

  /**
   * Called by the [Rx2Workflow] to obtain the next state transition.
   */
  fun onReact(
    state: S,
    events: Rx2EventChannel<E>
  ): Single<out Reaction<S, O>>

  /**
   * Called by the [Rx2Workflow] when the reactor is abandoned.
   */
  fun onAbandoned(state: S) {}
}

/**
 * Adapter to convert a [Rx2Reactor] to a [CoroutineReactor].
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
fun <S : Any, E : Any, O : Any> Rx2Reactor<S, E, O>.toCoroutineReactor() =
  object : CoroutineReactor<S, E, O> {
    override suspend fun react(
      state: S,
      events: ReceiveChannel<E>
    ): Reaction<S, O> {
      return this@toCoroutineReactor.onReact(state, events.asEventChannel())
          .await()
    }

    override fun abandon(state: S) = this@toCoroutineReactor.onAbandoned(state)

    override fun toString(): String =
      "${javaClass.simpleName}(${this@toCoroutineReactor.javaClass.name})"
  }

fun <S : Any, E : Any, O : Any> Rx2Reactor<S, E, O>.startWorkflow(initialState: S):
    Rx2Workflow<S, E, O> {
  return ReactorRx2Workflow(toCoroutineReactor(), initialState)
      .apply { start() }
}
