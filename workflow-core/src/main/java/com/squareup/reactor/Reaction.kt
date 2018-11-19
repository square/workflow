package com.squareup.reactor

/**
 * Basically a monad that indicates whether a [Reactor] should enter another state ([EnterState]), with
 * [EnterState.state] as the next state, or [FinishWith] the result [FinishWith.result].
 */
@Suppress("UNUSED")
sealed class Reaction<out S, out O>

/** Emit [state] as the next state to be passed to [Reactor.onReact]. */
data class EnterState<out S>(val state: S) : Reaction<S, Nothing>()

/**
 * Stop reacting. After this is returned from [Reactor.onReact], it won't be called anymore.
 * If the [Reactor] is hosted by a [ReactorDriver], causes [ReactorDriver.result] to emit [result].
 */
data class FinishWith<out O>(val result: O) : Reaction<Nothing, O>()
