package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Id

/**
 * Implemented by workflow states that represent nested workflows to be run by a [WorkflowPool].
 */
interface Delegating<S : Any, E : Any, O : Any> {
  /**
   * Uniquely identifies the delegate across the [WorkflowPool].
   * See [WorkflowPool.Type.makeId] for details.
   */
  val id: Id<S, E, O>

  /**
   * The current state of the delegate. Used as its initial state if it was not already
   * running; typically kept current via [com.squareup.workflow.onDelegateResult]
   */
  val delegateState: S
}
