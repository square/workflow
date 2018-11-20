package com.squareup.workflow

import com.squareup.reactor.WorkflowInput

/**
 * Given a workflow state [S], converts it to a type suitable for use as a view model.
 * At the moment, this probably means [AnyScreen] or [LayeredScreen].
 */
interface Renderer<S : Any, E : Any, R : Any> {

  /**
   * Renders [state] and [workflow] as [R].
   *
   * When [S] is a [Delegating] state, you'll typically make recursive calls
   * to the [Renderer] for its [nested state][Delegating.delegateState],
   * finding the appropriate [WorkflowInput] to provide it via [WorkflowPool.input].
   */
  fun render(
    state: S,
    workflow: WorkflowInput<E>,
    workflows: WorkflowPool
  ): R
}
