/*
 * Copyright 2017 Square Inc.
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

import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Launcher
import kotlin.reflect.KClass

/**
 * Describes a workflow to be run by a [WorkflowPool]. Includes the
 * [state][RunWorkflow.state] the workflow should start in / was last
 * known to be in, or the [result][FinishedWorkflow.result] the workflow
 * reported when it finished.
 *
 * It's important to understand that a handle is basically just a name
 * of a workflow that may not yet be running. See the docs on [RunWorkflow]
 * for a description of how [WorkflowPool] starts the required workflow
 * on demand for you.
 */
sealed class WorkflowHandle<S : Any, E : Any, O : Any> {
  /**
   * Uniquely identifies the delegate across the [WorkflowPool].
   * See [WorkflowPool.Type.makeWorkflowId] for details.
   */
  abstract val id: Id<S, E, O>

  companion object {

    /**
     * Returns a [RunWorkflow] handle that will instruct a [WorkflowPool] to call
     * launch a workflow of the given type in the given [state].
     *
     * @param name allows multiple workflows of the same type to be managed at once. If
     * no name is specified, we unique only on the type itself.
     */
    inline fun <reified S : Any, reified E : Any, reified O : Any> getStarter(
      launcherType: KClass<out Launcher<S, E, O>>,
      state: S,
      name: String = ""
    ): RunWorkflow<S, E, O> {
      return RunWorkflow(launcherType.makeWorkflowId(name), state)
    }
  }
}

/**
 * Handle that instructs [WorkflowPool.workflowUpdate] to start running the described
 * workflow from the given [state], if it isn't running already, and return
 * a new [WorkflowHandle] when the workflow's state changes from the given,
 * or it produces a result.
 *
 * In a [Reactor.onReact] implementation, pass the handle to
 * `WorkflowPool.`[workflowUpdate()] inside a `select` block
 * to start the workflow, handle its state updates, and handle the result
 * when it finishes.
 *
 * If you're using an Rx2 `EventChannel`, you'll pass the handle to `onWorkflowUpdate`
 * inside an `events.select {}` block.
 *
 * The easiest way to create instances is via [WorkflowHandle.getStarter].
 */
data class RunWorkflow<S : Any, E : Any, O : Any>(
  override val id: Id<S, E, O>,
  val state: S
) : WorkflowHandle<S, E, O>()

/**
 * Handle returned by a [WorkflowPool.workflowUpdate] when a workflow previously
 * started for a [RunWorkflow] handle finishes its work and reports a [result].
 */
data class FinishedWorkflow<S : Any, E : Any, O : Any>(
  override val id: Id<S, E, O>,
  val result: O
) : WorkflowHandle<S, E, O>()
