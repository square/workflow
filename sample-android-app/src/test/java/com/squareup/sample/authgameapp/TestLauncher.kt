package com.squareup.sample.authgameapp

import com.squareup.workflow.FinishWith
import com.squareup.workflow.Reaction
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.rx2.discreteStateWorkflow
import io.reactivex.subjects.PublishSubject

/**
 * Launches pretend [Workflow] instances for tests. Move them through
 * states by pushing [Reaction]s into [reactions].
 */
open class TestLauncher<S : Any, E : Any, O : Any> : Launcher<S, E, O> {
  val reactions = PublishSubject.create<Reaction<S, O>>()

  override fun launch(
    initialState: S,
    workflows: WorkflowPool
  ): Workflow<S, E, O> = workflows.discreteStateWorkflow(initialState, "TestLauncher") { _, _, _ ->
    reactions
        .takeUntil { it is FinishWith<O> }
        .take(1)
        .firstOrError()
  }
}
