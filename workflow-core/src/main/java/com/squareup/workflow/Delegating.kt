package com.squareup.workflow

import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Type

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

/**
 * Make an ID for the [workflowType] of this [Delegating].
 *
 * This is a convenience function intended to be used inside delegating states, to initialize
 * their [Delegating.id] property.
 *
 * @see Type.makeId
 */
@Suppress("unused")
inline fun <reified S : Any, reified E : Any, reified O : Any>
    Delegating<S, E, O>.makeId(name: String = ""): Id<S, E, O> =
// We can't use id.type since ID hasn't been initialized yet.
  Type(S::class, E::class, O::class).makeId(name)
