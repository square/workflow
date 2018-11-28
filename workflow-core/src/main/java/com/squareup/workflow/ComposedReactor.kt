package com.squareup.workflow

import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * See `ComposedReactor` in the Rx1 module.
 */
interface ComposedReactor<S : Any, E : Any, out O : Any> : WorkflowPool.Launcher<S, E, O> {
  suspend fun onReact(
    state: S,
    events: ReceiveChannel<E>,
    workflows: WorkflowPool
  ): Reaction<S, O>
}

/**
 * Use this to implement [WorkflowPool.Launcher.launch].
 */
@Suppress("DEPRECATION")
fun <S : Any, E : Any, O : Any> ComposedReactor<S, E, O>.doLaunch(
  initialState: S,
  workflows: WorkflowPool
): Workflow<S, E, O> {
  return object : Reactor<S, E, O> {
    override suspend fun react(
      state: S,
      events: ReceiveChannel<E>
    ): Reaction<S, O> {
      return onReact(state, events, workflows)
    }

    override fun abandon(state: S) {
      if (state is Delegating<*, *, *>) workflows.abandonDelegate(state.id)
    }
  }.startWorkflow(initialState)
}
