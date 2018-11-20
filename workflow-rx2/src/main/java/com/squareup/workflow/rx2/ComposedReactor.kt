package com.squareup.workflow.rx2

import com.squareup.workflow.Delegating
import com.squareup.workflow.Reaction
import com.squareup.workflow.WorkflowPool
import io.reactivex.Single

/**
 * See `ComposedReactor` in the Rx1 module.
 */
interface ComposedReactor<S : Any, E : Any, out O : Any> : WorkflowPool.Launcher<S, E, O> {
  fun onReact(
    state: S,
    events: EventChannel<E>,
    workflows: WorkflowPool
  ): Single<out Reaction<S, O>>
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
    override fun onReact(
      state: S,
      events: EventChannel<E>
    ): Single<out Reaction<S, O>> {
      return onReact(state, events, workflows)
    }

    override fun onAbandoned(state: S) {
      if (state is Delegating<*, *, *>) workflows.abandonDelegate(state.id)
    }
  }.startWorkflow(initialState)
}
