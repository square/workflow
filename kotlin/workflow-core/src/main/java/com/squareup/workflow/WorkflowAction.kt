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

/**
 * A function that can change the current state of a [Workflow] by returning a new one, and
 * also optionally emit an output.
 */
interface WorkflowAction<StateT, out OutputT : Any> : (StateT) -> Pair<StateT, OutputT?> {

  override operator fun invoke(state: StateT): Pair<StateT, OutputT?>

  companion object {

    /**
     * Creates a [WorkflowAction] from the [update] lambda.
     * The returned object will include the string returned from [name] in its [toString].
     *
     * @param name Function that returns a string describing the update for debugging.
     * @param update Function that defines the workflow update.
     */
    inline operator fun <StateT, OutputT : Any> invoke(
      crossinline name: () -> String,
      crossinline update: (StateT) -> Pair<StateT, OutputT?>
    ): WorkflowAction<StateT, OutputT> = object : WorkflowAction<StateT, OutputT> {
      override fun invoke(state: StateT): Pair<StateT, OutputT?> = update(state)
      override fun toString(): String = "WorkflowAction(${name()})@${hashCode()}"
    }

    /**
     * Returns a [WorkflowAction] that does nothing: no output will be emitted, and `render` will be
     * called again with the same `state` as last time.
     *
     * Use this to, for example, ignore the output of a child workflow or worker.
     */
    fun <StateT, OutputT : Any> noAction(): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "noop" }) { Pair(it, null) }

    /**
     * Convenience function that returns a [WorkflowAction] that will just set the state to [newState]
     * (without considering the current state) and optionally emit an output.
     */
    fun <StateT, OutputT : Any> enterState(
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "enterState($newState, $emittingOutput)" }) {
        Pair(newState, emittingOutput)
      }

    /**
     * Convenience function that returns a [WorkflowAction] that will just set the state to [newState]
     * (without considering the current state) and optionally emit an output.
     */
    fun <StateT, OutputT : Any> enterState(
      name: String,
      newState: StateT,
      emittingOutput: OutputT? = null
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "enterState($name, $newState, $emittingOutput)" }) {
        Pair(newState, emittingOutput)
      }

    /**
     * Convenience function to implement [WorkflowAction] without returning the output.
     */
    fun <StateT, OutputT : Any> modifyState(
      name: () -> String,
      emittingOutput: OutputT? = null,
      modify: (StateT) -> StateT
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "modifyState(${name()}, $emittingOutput)" }) {
        Pair(modify(it), emittingOutput)
      }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    fun <StateT, OutputT : Any> emitOutput(output: OutputT): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "emitOutput($output)" }) { Pair(it, output) }

    /**
     * Convenience function to implement [WorkflowAction] without changing the state.
     */
    fun <StateT, OutputT : Any> emitOutput(
      name: String,
      output: OutputT
    ): WorkflowAction<StateT, OutputT> =
      WorkflowAction({ "emitOutput($name, $output)" }) { Pair(it, output) }
  }
}
