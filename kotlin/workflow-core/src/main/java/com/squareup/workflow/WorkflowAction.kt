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
@file:Suppress("DeprecatedCallableAddReplaceWith")

package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Mutator

/**
 * A function that can change the current state of a [Workflow] by returning a new one, and
 * also optionally emit an output.
 */
interface WorkflowAction<StateT, out OutputT : Any> {
  class Mutator<S>(var state: S)

  fun Mutator<StateT>.apply(): OutputT?

  companion object {

    /**
     * Convenience to create a [WorkflowAction] from the [apply] lambda.
     * The returned object will include the string returned from [name] in its [toString].
     *
     * Most useful in smaller workflows with few actions. Large state machines are better
     * served with a sealed class hierarchy implementing the [WorkflowAction] interface directly.
     *
     * @param name Function that returns a string describing the update for debugging.
     * @param apply Function that defines the workflow update.
     */
    inline operator fun <StateT, OutputT : Any> invoke(
      crossinline name: () -> String = { "" },
      crossinline apply: Mutator<StateT>.() -> OutputT?
    ): WorkflowAction<StateT, OutputT> = object : WorkflowAction<StateT, OutputT> {
      override fun Mutator<StateT>.apply() = apply.invoke(this)
      override fun toString(): String = "WorkflowAction(${name()})@${hashCode()}"
    }

    inline operator fun <StateT, OutputT : Any> invoke(
      crossinline apply: Mutator<StateT>.() -> OutputT?
    ): WorkflowAction<StateT, OutputT> = object : WorkflowAction<StateT, OutputT> {
      override fun Mutator<StateT>.apply() = apply.invoke(this)
    }

    /**
     * Returns a [WorkflowAction] that does nothing: no output will be emitted, and `render` will be
     * called again with the same `state` as last time.
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
    @Deprecated("Use WorkflowAction.invoke, or implement the interface directly.")
    fun <StateT, OutputT : Any> enterState(
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "enterState($newState, $emittingOutput)" }) {
        state = newState
        return@WorkflowAction emittingOutput
      }

    /**
     * Convenience function that returns a [WorkflowAction] that will just set the state to [newState]
     * (without considering the current state) and optionally emit an output.
     */
    @Deprecated("Use WorkflowAction.invoke, or implement the interface directly.")
    fun <StateT, OutputT : Any> enterState(
      name: String,
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "enterState($name, $newState, $emittingOutput)" }) {
        state = newState
        return@WorkflowAction emittingOutput
      }

    /**
     * Convenience function to implement [WorkflowAction] without returning the output.
     */
    @Deprecated("Use WorkflowAction.invoke, or implement the interface directly.")
    fun <StateT, OutputT : Any> modifyState(
      name: () -> String,
      emittingOutput: OutputT? = null,
      modify: (StateT) -> StateT
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "modifyState(${name()}, $emittingOutput)" }) {
        state = modify(state)
        return@WorkflowAction emittingOutput
      }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    @Deprecated("Use WorkflowAction.invoke, or implement the interface directly.")
    fun <StateT, OutputT : Any> emitOutput(output: OutputT): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "emitOutput($output)" }) { output }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    @Deprecated("Use WorkflowAction.invoke, or implement the interface directly.")
    fun <StateT, OutputT : Any> emitOutput(
      name: String,
      output: OutputT
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "emitOutput($name, $output)" }) { output }

    private val NO_ACTION = WorkflowAction<Any, Any>({ "noAction" }) { null }
  }
}

fun <StateT, OutputT : Any> WorkflowAction<StateT, OutputT>.applyTo(
  state: StateT
): Pair<StateT, OutputT?> {
  val box = Mutator(state)
  val output = box.apply()
  return Pair(box.state, output)
}
