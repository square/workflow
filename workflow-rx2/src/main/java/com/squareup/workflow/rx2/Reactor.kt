package com.squareup.workflow.rx2

import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.doLaunch
import com.squareup.workflow.startRootWorkflow
import io.reactivex.Single
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.rx2.await
import com.squareup.workflow.Reactor as CoroutineReactor

/**
 * An Rx2 adapter for [com.squareup.workflow.Reactor].
 */
interface Reactor<S : Any, E : Any, out O : Any> : WorkflowPool.Launcher<S, E, O> {
  fun onReact(
    state: S,
    events: EventChannel<E>,
    workflows: WorkflowPool
  ): Single<out Reaction<S, O>>

  override fun launch(
    initialState: S,
    workflows: WorkflowPool
  ): Workflow<S, E, O> = doLaunch(initialState, workflows)
}

/**
 * Use this to implement [WorkflowPool.Launcher.launch].
 */
fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.doLaunch(
  initialState: S,
  workflows: WorkflowPool
): Workflow<S, E, O> = toCoroutineReactor().doLaunch(initialState, workflows)

/**
 * Use this only to create a top-level workflow. If you're implementing
 * [WorkflowPool.Launcher.launch], use [doLaunch].
 */
fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.startRootWorkflow(
  initialState: S
): Workflow<S, E, O> = toCoroutineReactor().startRootWorkflow(initialState)

/**
 * Adapter to convert a [Reactor] to a [Reactor].
 */
fun <S : Any, E : Any, O : Any> Reactor<S, E, O>.toCoroutineReactor() =
  object : CoroutineReactor<S, E, O> {
    override suspend fun onReact(
      state: S,
      events: ReceiveChannel<E>,
      workflows: WorkflowPool
    ): Reaction<S, O> = this@toCoroutineReactor.onReact(state, events.asEventChannel(), workflows)
        .await()

    override fun launch(
      initialState: S,
      workflows: WorkflowPool
    ): Workflow<S, E, O> = this@toCoroutineReactor.launch(initialState, workflows)

    override fun toString(): String =
      "${javaClass.simpleName}(${this@toCoroutineReactor.javaClass.name})"
  }
