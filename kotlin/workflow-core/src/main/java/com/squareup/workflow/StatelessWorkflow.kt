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
 * A composable object that can [handle events][WorkflowContext.onEvent],
 * [delegate to children][WorkflowContext.compose], and [subscribe][onReceive] to arbitrary streams
 * from the outside world.
 *
 * The basic purpose of a `Workflow` is to take some [input][InputT] and return a
 * [rendering][RenderingT]. To that end, a workflow may recursively ask other workflows to render
 * themselves, subscribe to data streams from the outside world, and handle events both from its
 * [renderings][WorkflowContext.onEvent] and from workflows it's delegated to (its "children"). A
 * `Workflow` may also emit [output events][OutputT] up to its parent `Workflow`.
 *
 * Workflows form a tree, where each workflow can have zero or more child workflows. Child workflows
 * are started as necessary whenever another workflow asks for them, and are cleaned up automatically
 * when they're no longer needed. [Input][InputT] propagates down the tree, [outputs][OutputT] and
 * [renderings][RenderingT] propagate up the tree.
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
 * A convenience function to implement [StatelessWorkflow] by just passing the
 * [compose][StatelessWorkflow.compose] function as a lambda.
 *
 * Note that while a stateless workflow doesn't have any _internal_ state of its own, it may use
 * [input][InputT] received from its parent, and it may compose child workflows that do have their own
 * internal state.
 */
@Suppress("FunctionName")
fun <InputT : Any, OutputT : Any, RenderingT : Any> StatelessWorkflow(
  compose: (
    input: InputT,
    context: WorkflowContext<Nothing, OutputT>
  ) -> RenderingT
): StatelessWorkflow<InputT, OutputT, RenderingT> =
  object : StatelessWorkflow<InputT, OutputT, RenderingT>() {
    override fun compose(
      input: InputT,
      context: WorkflowContext<Nothing, OutputT>
    ): RenderingT = compose.invoke(input, context)
  }

/**
 * A version of [StatelessWorkflow] that doesn't have any input data either.
 */
@Suppress("FunctionName")
fun <OutputT : Any, RenderingT : Any> StatelessWorkflow(
  compose: (context: WorkflowContext<Nothing, OutputT>) -> RenderingT
): StatelessWorkflow<Unit, OutputT, RenderingT> =
  StatelessWorkflow { _: Unit, context: WorkflowContext<Nothing, OutputT> ->
    compose(context)
  }
