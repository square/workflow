package com.squareup.sample.mainworkflow

import com.squareup.workflow.legacy.FinishWith
import com.squareup.workflow.legacy.Reaction
import com.squareup.workflow.legacy.Workflow
import com.squareup.workflow.legacy.WorkflowPool
import com.squareup.workflow.legacy.WorkflowPool.Launcher
import com.squareup.workflow.legacy.rx2.EventChannel
import com.squareup.workflow.legacy.rx2.Reactor
import com.squareup.workflow.legacy.rx2.doLaunch
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

/**
 * Launches pretend [Workflow] instances for tests. Move them through
 * states by pushing [Reaction]s into [reactions].
 */
open class TestLauncher<S : Any, E : Any, O : Any> : Launcher<S, E, O> {
  private val reactor = object : Reactor<S, E, O> {
    override fun onReact(
      state: S,
      events: EventChannel<E>,
      workflows: WorkflowPool
    ): Single<out Reaction<S, O>> {
      return reactions
          .takeUntil { it is FinishWith<O> }
          .take(1)
          .firstOrError()
    }
  }

  val reactions = PublishSubject.create<Reaction<S, O>>()

  override fun launch(
    initialState: S,
    workflows: WorkflowPool
  ): Workflow<S, E, O> {
    return reactor.doLaunch(initialState, workflows)
  }
}
