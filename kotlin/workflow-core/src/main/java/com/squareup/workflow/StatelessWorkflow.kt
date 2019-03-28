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

import com.squareup.workflow.WorkflowAction.Companion.emitOutput

/**
 * Minimal implementation of [Workflow] that maintains no state of its own.
 *
 * @param InputT Typically a data class that is used to pass configuration information or bits of
 * state that the workflow can always get from its parent and needn't duplicate in its own state.
 * May be [Unit] if the workflow does not need any input data.
 *
 * @param OutputT Typically a sealed class that represents "events" that this workflow can send
 * to its parent.
 * May be [Nothing] if the workflow doesn't need to emit anything.
 *
 * @param RenderingT The value returned to this workflow's parent during [composition][compose].
 * Typically represents a "view" of this workflow's input, current state, and children's renderings.
 * A workflow that represents a UI component may use a view model as its rendering type.
 *
 * @see StatefulWorkflow
 */
abstract class StatelessWorkflow<InputT : Any, OutputT : Any, RenderingT : Any> :
    Workflow<InputT, OutputT, RenderingT> {

  /**
   * Called at least once any time one of the following things happens:
   *  - This workflow's [input] changes (via the parent passing a different one in).
   *  - A descendant (immediate or transitive child) workflow:
   *    - Changes its internal state.
   *    - Emits an output.
   *
   * **Never call this method directly.** To get the rendering from a child workflow, pass the child
   * and any required input to [WorkflowContext.compose].
   *
   * This method *should not* have any side effects, and in particular should not do anything that
   * blocks the current thread. It may be called multiple times for the same state. It must do all its
   * work by calling methods on [context].
   */
  abstract fun compose(
    input: InputT,
    context: WorkflowContext<Nothing, OutputT>
  ): RenderingT

  /**
   * Satisfies the [Workflow] interface by wrapping `this` in a [StatefulWorkflow] with `Unit`
   * state.
   */
  final override fun asStatefulWorkflow(): StatefulWorkflow<InputT, *, OutputT, RenderingT> =
    object : StatefulWorkflow<InputT, Unit, OutputT, RenderingT>() {
      override fun initialState(input: InputT) = Unit

      @Suppress("UNCHECKED_CAST")
      override fun compose(
        input: InputT,
        state: Unit,
        context: WorkflowContext<Unit, OutputT>
      ): RenderingT = compose(input, context as WorkflowContext<Nothing, OutputT>)

      override fun snapshotState(state: Unit) = Snapshot.EMPTY
      override fun restoreState(snapshot: Snapshot) = Unit
    }
}

/**
 * Returns a stateless [Workflow] via the given [compose] function.
 *
 * Note that while the returned workflow doesn't have any _internal_ state of its own, it may use
 * [input][InputT] received from its parent, and it may compose child workflows that do have
 * their own internal state.
 */
fun <InputT : Any, OutputT : Any, RenderingT : Any> Workflow.Companion.stateless(
  compose: (
    input: InputT,
    context: WorkflowContext<Nothing, OutputT>
  ) -> RenderingT
): Workflow<InputT, OutputT, RenderingT> =
  object : StatelessWorkflow<InputT, OutputT, RenderingT>() {
    override fun compose(
      input: InputT,
      context: WorkflowContext<Nothing, OutputT>
    ): RenderingT = compose.invoke(input, context)
  }

/**
 * Returns a stateless [Workflow] that ignores input via the given [compose] function.
 *
 * Note that while the returned workflow doesn't have any _internal_ state of its own, it may
 * compose child workflows that do have their own internal state.
 */
fun <OutputT : Any, RenderingT : Any> Workflow.Companion.stateless(
  compose: (context: WorkflowContext<Nothing, OutputT>) -> RenderingT
): Workflow<Unit, OutputT, RenderingT> =
  Workflow.stateless { _: Unit, context: WorkflowContext<Nothing, OutputT> ->
    compose(context)
  }

/**
 * Returns a workflow that does nothing but echo the given [rendering].
 * Handy for testing.
 */
fun <OutputT : Any, RenderingT : Any> Workflow.Companion.rendering(
  rendering: RenderingT
): Workflow<Unit, OutputT, RenderingT> =
  stateless { _: Unit, _: WorkflowContext<Nothing, OutputT> -> rendering }

/**
 * Uses the given [function][transform] to transform a [Workflow] that
 * renders [FromRenderingT] to one renders [ToRenderingT],
 */
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
fun <InputT : Any, OutputT : Any, FromRenderingT : Any, ToRenderingT : Any>
    Workflow<InputT, OutputT, FromRenderingT>.mapRendering(
      transform: (FromRenderingT) -> ToRenderingT
    ): Workflow<InputT, OutputT, ToRenderingT> = Workflow.stateless { input, context ->
  // @formatter:on
  context.compose(this@mapRendering, input) { emitOutput(it) }
      .let(transform)
}
