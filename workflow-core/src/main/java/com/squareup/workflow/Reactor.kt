package com.squareup.workflow

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

/**
 * Implements a state machine where the states are represented by objects of type (or subclasses of
 * type) `S`.
 *
 * > ***Using [Reactor] directly is not currently supported. Subclass `Reactor` instead.***
 *
 * The state machine can be initialized with an arbitrary value of type `I` which is converted to
 * the initial state by the method [onStart].
 *
 * When the state machine is executed, each consecutive state will be passed to [onReact], which
 * returns a [Reaction] indicating what to do next:
 *   * `EnterState(nextState)`: Call [onReact] again with `nextState`.
 *   * `FinishWith(result)`: The state machine is in a terminal state and has a result of type `O`.
 *
 * Example:
 * ```
 * private val onFoo = Channel<Unit>()
 * private val onBar = Channel<Unit>()
 *
 * private fun handleFoo(): Reaction<…> { … }
 *
 * fun onFooClicked() = onFoo.send(Unit)
 *
 * override suspend fun onReact(state: MyState): Reaction<…> = when(state) {
 *   JustFooState -> select {
 *     onFoo.onReceive { handleFoo() }
 *   }
 *
 *   FooOrBarState -> select {
 *     onFoo.onReceive { handleFoo() }
 *     onBar.onReceive { EnterState(JustFooState) }
 *   }
 *
 *   BarStateAsync -> select {
 *     onBar.onReceive { async { EnterState(JustFooState) }.await() }
 *   }
 * }
 * ```
 *
 * If you need to perform asynchronous work to handle an event,
 * ```
 * private val onFoo = Channel<Unit>()
 * private val onBar = Channel<Unit>()
 *
 * private suspend fun handleFooAsync(): Reaction<…> { … }
 *
 * override suspend fun onReact(state: MyState): Reaction<…> = when(state) {
 *   JustFooState -> select {
 *     onFoo.onReceive { handleFooAsync() }
 *   }
 *
 *   FooOrBarState -> select {
 *     onFoo.onReceive { handleFooAsync() }
 *     onBar.onReceive { EnterState(JustFooState) }
 *   }
 * }
 * ```
 *
 * If, like the above example, all of your states handle events, you can pull the [select]
 * call up:
 * ```
 * override suspend fun onReact(state: MyState): Reaction<…> = select {
 *   when(state) {
 *     JustFooState -> onFoo.onReceive { handleFoo() }
 *
 *     FooOrBarState -> {
 *       onFoo.onReceive { handleFoo() }
 *       onBar.onReceive { EnterState(JustFooState) }
 *     }
 *   }
 * }
 * ```
 *
 * For more information, including a how-to guide for writing Reactors, please see the `README.md` file
 * in this package.
 *
 * @param S State type
 * @param E Event type. If your Reactor doesn't have any events, use `Nothing`
 * @param O Output (result) type
 */
interface Reactor<S : Any, E : Any, out O : Any> {
  /**
   * Called by the `Workflow` to obtain the next state transition.
   */
  suspend fun react(
    state: S,
    events: ReceiveChannel<E>
  ): Reaction<S, O>

  /**
   * Called from `Workflow.cancel` when the reactor is abandoned. Note that this
   * method is called _before_ `Workflow.state` completes.
   *
   * Optional, has default empty implementation.
   */
  fun abandon(state: S) {}
}

fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.startWorkflow(
  initialState: S,
  context: CoroutineContext = EmptyCoroutineContext
): Workflow<S, E, O> =
  ReactorWorkflow(this, initialState, context)
