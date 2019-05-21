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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy

/**
 * Given a workflow state [S], converts it to a type suitable for use as a view model.
 */
@Deprecated("Use com.squareup.workflow.Workflow")
interface Renderer<S : Any, E : Any, R : Any> {

  /**
   * Renders [state] and [workflow] as [R].
   *
   * Any [Running] field of [S] can be handled via a recursive calls
   * to the [Renderer] appropriate for its [state][Running.handle],
   * using [WorkflowPool.input] to find the right [WorkflowInput].
   *
   * By making the parent [Renderer] responsible for finding the [WorkflowInput] to
   * be used by the child, we allow the parent to intercept the child's events. E.g.,
   * a modal parent can disable a child by passing it [WorkflowInput.disabled].
   */
  fun render(
    state: S,
    workflow: WorkflowInput<E>,
    workflows: WorkflowPool
  ): R
}

/**
 * Convenience for the usual case where we're rendering a [handle][Running] and sending
 * events to the [WorkflowInput] it identifies.
 */
fun <S : Any, E : Any, O : Any, R : Any> Renderer<S, E, R>.render(
  handle: WorkflowPool.Handle<S, E, O>,
  workflows: WorkflowPool
): R = render(handle.state, workflows.input(handle), workflows)
