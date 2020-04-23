/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Companion.toString
import com.squareup.workflow.WorkflowAction.Updater

/**
 * An atomic operation that updates the state of a [Workflow], and also optionally emits an output.
 */
interface WorkflowAction<StateT, out OutputT : Any> {
  @Deprecated("Use Updater")
  class Mutator<S>(var state: S)

  /**
   * The context for calls to [WorkflowAction.apply]. Allows the action to set the
   * [nextState], and to emit the [setOutput].
   *
   * @param nextState the state that the workflow should move to. Default is the current state.
   */
  class Updater<S, in O : Any>(var nextState: S) {
    internal var output: @UnsafeVariance O? = null

    /**
     * Sets the value the workflow will emit as output when this action is applied.
     * If this method is not called, there will be no output.
     */
    fun setOutput(output: O) {
      this.output = output
    }
  }

  @Suppress("DEPRECATION")
  @Deprecated("Implement Updater.apply")
  fun Mutator<StateT>.apply(): OutputT? {
    throw UnsupportedOperationException()
  }

  @Suppress("DEPRECATION")
  fun Updater<StateT, OutputT>.apply() {
    val mutator = Mutator(nextState)
    mutator.apply()
        ?.let { setOutput(it) }
    nextState = mutator.state
  }

  companion object {
    /**
     * Returns a [WorkflowAction] that does nothing: no output will be emitted, and
     * the state will not change.
     *
     * Use this to, for example, ignore the output of a child workflow or worker.
     */
    @Suppress("UNCHECKED_CAST")
    fun <StateT, OutputT : Any> noAction(): WorkflowAction<StateT, OutputT> =
      NO_ACTION as WorkflowAction<StateT, OutputT>

    /**
     * Convenience function that returns a [WorkflowAction] that will just set the state to [newState]
     * (without considering the current state) and optionally emit an output.
     */
    @Deprecated(
        message = "Use action",
        replaceWith = ReplaceWith(
            expression = "action { nextState = newState }",
            imports = arrayOf("com.squareup.workflow.action")
        )
    )
    fun <StateT, OutputT : Any> enterState(
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      action({ "enterState($newState, $emittingOutput)" }) {
        nextState = newState
        emittingOutput?.let { setOutput(it) }
      }

    /**
     * Convenience function that returns a [WorkflowAction] that will just set the state to [newState]
     * (without considering the current state) and optionally emit an output.
     */
    @Deprecated(
        message = "Use action",
        replaceWith = ReplaceWith(
            expression = "action { nextState = newState }",
            imports = arrayOf("com.squareup.workflow.action")
        )
    )
    fun <StateT, OutputT : Any> enterState(
      name: String,
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      action({ "enterState($name, $newState, $emittingOutput)" }) {
        nextState = newState
        emittingOutput?.let { setOutput(it) }
      }

    /**
     * Convenience function to implement [WorkflowAction] without returning the output.
     */
    @Deprecated(
        message = "Use action",
        replaceWith = ReplaceWith(
            expression = "action(name) { nextState = state }",
            imports = arrayOf("com.squareup.workflow.action")
        )
    )
    fun <StateT, OutputT : Any> modifyState(
      name: () -> String,
      emittingOutput: OutputT? = null,
      modify: (StateT) -> StateT
    ): WorkflowAction<StateT, OutputT> =
      action({ "modifyState(${name()}, $emittingOutput)" }) {
        nextState = modify(nextState)
        emittingOutput?.let { setOutput(it) }
      }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    @Deprecated(
        message = "Use action",
        replaceWith = ReplaceWith(
            expression = "action { setOutput(output) }",
            imports = arrayOf("com.squareup.workflow.action")
        )
    )
    fun <StateT, OutputT : Any> emitOutput(output: OutputT): WorkflowAction<StateT, OutputT> =
      action({ "emitOutput($output)" }) { setOutput(output) }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    @Deprecated(
        message = "Use action",
        replaceWith = ReplaceWith(
            expression = "action { setOutput(output) }",
            imports = arrayOf("com.squareup.workflow.action")
        )
    )
    fun <StateT, OutputT : Any> emitOutput(
      name: String,
      output: OutputT
    ): WorkflowAction<StateT, OutputT> =
      action({ "emitOutput($name, $output)" }) { setOutput(output) }

    private val NO_ACTION = action<Any, Any>({ "noAction" }) { }
  }
}

/**
 * Creates a [WorkflowAction] from the [apply] lambda.
 * The returned object will include the string returned from [name] in its [toString].
 *
 * If defining actions within a [StatefulWorkflow], use the [StatefulWorkflow.workflowAction]
 * extension instead, to do this without being forced to repeat its parameter types.
 *
 * @param name A string describing the update for debugging.
 * @param apply Function that defines the workflow update.
 *
 * @see StatelessWorkflow.action
 * @see StatefulWorkflow.action
 */
inline fun <StateT, OutputT : Any> action(
  name: String = "",
  crossinline apply: Updater<StateT, OutputT>.() -> Unit
) = action({ name }, apply)

/**
 * Creates a [WorkflowAction] from the [apply] lambda.
 * The returned object will include the string returned from [name] in its [toString].
 *
 * If defining actions within a [StatefulWorkflow], use the [StatefulWorkflow.workflowAction]
 * extension instead, to do this without being forced to repeat its parameter types.
 *
 * @param name Function that returns a string describing the update for debugging.
 * @param apply Function that defines the workflow update.
 *
 * @see StatelessWorkflow.action
 * @see StatefulWorkflow.action
 */
inline fun <StateT, OutputT : Any> action(
  crossinline name: () -> String,
  crossinline apply: Updater<StateT, OutputT>.() -> Unit
): WorkflowAction<StateT, OutputT> = object : WorkflowAction<StateT, OutputT> {
  override fun Updater<StateT, OutputT>.apply() = apply.invoke(this)
  override fun toString(): String = "WorkflowAction(${name()})@${hashCode()}"
}

fun <StateT, OutputT : Any> WorkflowAction<StateT, OutputT>.applyTo(
  state: StateT
): Pair<StateT, OutputT?> {
  val updater = Updater<StateT, OutputT>(state)
  updater.apply()
  return Pair(updater.nextState, updater.output)
}
