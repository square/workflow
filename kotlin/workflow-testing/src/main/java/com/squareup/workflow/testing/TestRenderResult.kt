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
package com.squareup.workflow.testing

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.applyTo
import com.squareup.workflow.internal.Behavior
import com.squareup.workflow.internal.Behavior.WorkerCase
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.WorkflowId

/**
 * Represents the result of running a single render pass on a workflow.
 *
 * @param rendering The actual [RenderingT] value returned from the workflow's `render` method.
 * @param state The [StateT] passed into the `render` method.
 * @param behavior The [Behavior] generated from the
 * [RenderContext][com.squareup.workflow.RenderContext].
 */
class TestRenderResult<StateT, OutputT : Any, RenderingT> internal constructor(
  val rendering: RenderingT,
  internal val state: StateT,
  private val behavior: Behavior<StateT, OutputT>
) {

  /**
   * Throws an [AssertionError] if the render pass did not render [this@assertWorkflowRendered]
   * with [withKey].
   */
  fun <CInput, COutputT : Any, CRenderingT> Workflow<CInput, COutputT, CRenderingT>.assertRendered(
    withKey: String = ""
  ) {
    findWorkflowCase(this, withKey)
  }

  /**
   * Asserts that  this workflow was rendered with the given [key] and then executes the output
   * handler with the given [output] (as an [Output]). Returns the new state and output returned by
   * the output handler.
   */
  fun <CPropsT, COutputT : Any, CRenderingT> Workflow<CPropsT, COutputT, CRenderingT>.handleOutput(
    output: COutputT,
    key: String = ""
  ): Pair<StateT, OutputT?> {
    val case = findWorkflowCase(this, key)
    val action = case.acceptChildOutput(output)
    return action.applyTo(state)
  }

  /**
   * Throws an [AssertionError] if the render pass did not run this worker with [withKey].
   */
  fun <T : Any> Worker<T>.assertRan(withKey: String = "") {
    findWorkerCase(this, withKey)
  }

  /**
   * Asserts that this worker was ran with the given [key] and then executes the output handler
   * with the given [output] (as an [Output]). Returns the new state and output returned by the
   * output handler.
   */
  fun <T : Any> Worker<T>.handleOutput(
    output: T,
    key: String = ""
  ): Pair<StateT, OutputT?> = executeWorkerAction(this, output, key)

  /**
   * Throws an [AssertionError] if any [Workflow]s were rendered.
   */
  fun assertNoWorkflowsRendered() {
    behavior.childCases.let {
      if (it.isNotEmpty()) {
        throw AssertionError("Expected no workflows to be rendered, but ${it.size} were.")
      }
    }
  }

  /**
   * Throws an [AssertionError] if any [Worker]s were run.
   */
  fun assertNoWorkersRan() {
    behavior.workerCases.let {
      if (it.isNotEmpty()) {
        throw AssertionError("Expected no workflows to be rendered, but ${it.size} were.")
      }
    }
  }

  /**
   * Call this after invoking one of the [event handlers][RenderContext.onEvent] on your rendering
   * to get the result of handling that event.
   *
   * E.g.
   * ```
   * rendering.let {
   *   assertTrue(it is FooRendering)
   *   it.onFooEvent(Unit)
   *   val (state, output) = getEventResult()
   * }
   * ```
   */
  fun getEventResult(): Pair<StateT, OutputT?> {
    @Suppress("EXPERIMENTAL_API_USAGE")
    val action = behavior.nextActionFromEvent.getCompleted()
    return action.applyTo(state)
  }

  private fun <T : Any> executeWorkerAction(
    worker: Worker<T>,
    outputOrFinished: T,
    key: String = ""
  ): Pair<StateT, OutputT?> {
    val case = findWorkerCase(worker, key)
    val action = case.acceptUpdate(outputOrFinished)
    return action.applyTo(state)
  }

  private fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> findWorkflowCase(
    workflow: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    key: String
  ): WorkflowOutputCase<ChildPropsT, ChildOutputT, StateT, OutputT> {
    val id = WorkflowId(workflow, key)
    @Suppress("UNCHECKED_CAST")
    return behavior.childCases.singleOrNull { it.id == id }
        as WorkflowOutputCase<ChildPropsT, ChildOutputT, StateT, OutputT>?
        ?: throw AssertionError("Expected workflow to be rendered: $workflow (key=\"$key\")")
  }

  private fun <T> findWorkerCase(
    worker: Worker<T>,
    key: String
  ): WorkerCase<T, StateT, OutputT> {
    @Suppress("UNCHECKED_CAST")
    return behavior.workerCases.singleOrNull { it.worker.doesSameWorkAs(worker) && it.key == key }
        as WorkerCase<T, StateT, OutputT>?
        ?: throw AssertionError("Expected worker to be rendered: $worker (key=\"$key\")")
  }
}