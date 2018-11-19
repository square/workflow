package com.squareup.reactor.rx2

import com.squareup.reactor.Reaction
import com.squareup.workflow.Delegating
import com.squareup.workflow.Rx2Workflow
import com.squareup.workflow.WorkflowPool
import io.reactivex.Single

/**
 * See `ComposedReactor` in the Rx1 module.
 */
interface Rx2ComposedReactor<S : Any, E : Any, out O : Any> : WorkflowPool.Launcher<S, E, O> {
  fun onReact(
    state: S,
    events: Rx2EventChannel<E>,
    workflows: WorkflowPool
  ): Single<out Reaction<S, O>>
}

/**
 * Use this to implement [WorkflowPool.Launcher.launchRx2].
 */
@Suppress("DEPRECATION")
fun <S : Any, E : Any, O : Any> Rx2ComposedReactor<S, E, O>.doLaunch(
  initialState: S,
  workflows: WorkflowPool
): Rx2Workflow<S, E, O> {
  return object : Rx2Reactor<S, E, O> {
    override fun onReact(
      state: S,
      events: Rx2EventChannel<E>
    ): Single<out Reaction<S, O>> {
      return onReact(state, events, workflows)
    }

    override fun onAbandoned(state: S) {
      if (state is Delegating<*, *, *>) workflows.abandonDelegate(state.id)
    }
  }.startWorkflow(initialState)
}
