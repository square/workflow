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

import kotlinx.coroutines.CoroutineScope

/**
 * A composable, stateful object that can [handle events][RenderContext.onEvent],
 * [delegate to children][RenderContext.renderChild], [subscribe][onReceive] to arbitrary streams from
 * the outside world, and be [saved][snapshotState] to a serialized form to be restored later.
 *
 * The basic purpose of a `Workflow` is to take some [input][InputT] and return a
 * [rendering][RenderingT]. To that end, a workflow may keep track of internal [state][StateT],
 * recursively ask other workflows to render themselves, subscribe to data streams from the outside
 * world, and handle events both from its [renderings][RenderContext.onEvent] and from workflows
 * it's delegated to (its "children"). A `Workflow` may also emit [output events][OutputT] up to its
 * parent `Workflow`.
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
 * @param StateT Typically a data class that contains all of the internal state for this workflow.
 * The state is seeded via [input][InputT] in [initialState]. It can be [serialized][snapshotState]
 * and later used to restore the workflow. **Implementations of the `Workflow`
 * interface should not generally contain their own state directly.** They may inject objects like
 * instances of their child workflows, or network clients, but should not contain directly mutable
 * state. This is the only type parameter that a parent workflow needn't care about for its children,
 * and may just use star (`*`) instead of specifying it. May be [Unit] if the workflow does not have
 * any internal state (see [StatelessWorkflow]).
 *
 * @param OutputT Typically a sealed class that represents "events" that this workflow can send
 * to its parent.
 * May be [Nothing] if the workflow doesn't need to emit anything.
 *
 * @param RenderingT The value returned to this workflow's parent during [composition][render].
 * Typically represents a "view" of this workflow's input, current state, and children's renderings.
 * A workflow that represents a UI component may use a view model as its rendering type.
 *
 * @see StatelessWorkflow
 */
abstract class StatefulWorkflow<
    in InputT : Any,
    StateT : Any,
    out OutputT : Any,
    out RenderingT
    > : Workflow<InputT, OutputT, RenderingT> {

  /**
   * Called from [RenderContext.renderChild] when the state machine is first started, to get the
   * initial state.
   *
   * @param snapshot
   * If the workflow is being created fresh, [snapshot] will be null.
   * If the workflow is being restored from a [Snapshot], [snapshot] will be the last value
   * returned from [snapshotState], and implementations that return something other than
   * [Snapshot.EMPTY] should create their initial state by parsing their snapshot.
   * @param scope
   * The [CoroutineScope] in which this workflow lives. The scope will be cancelled when the
   * workflow is being torn down, so this scope can be used to start coroutines to track the
   * lifetime of the workflow "session".
   */
  abstract fun initialState(
    input: InputT,
    snapshot: Snapshot?
  ): StateT

  /**
   * Called from [RenderContext.renderChild] instead of [initialState] when the workflow is already
   * running. This allows the workflow to detect changes in input, and possibly change its state in
   * response. This method is called eagerly: `old` and `new` might be the same value, so it is up
   * to implementing code to perform any diffing if desired.
   *
   * Default implementation does nothing.
   */
  open fun onInputChanged(
    old: InputT,
    new: InputT,
    state: StateT
  ): StateT = state

  /**
   * Called at least once† any time one of the following things happens:
   *  - This workflow's [input] changes (via the parent passing a different one in).
   *  - This workflow's [state] changes.
   *  - A descendant (immediate or transitive child) workflow:
   *    - Changes its internal state.
   *    - Emits an output.
   *
   * **Never call this method directly.** To nest the rendering of a child workflow in your own,
   * pass the child and any required input to [RenderContext.renderChild].
   *
   * This method *should not* have any side effects, and in particular should not do anything that
   * blocks the current thread. It may be called multiple times for the same state. It must do all its
   * work by calling methods on [context].
   *
   * _† This method is guaranteed to be called *at least* once for every state, but may be called
   * multiple times. Allowing this method to be invoked multiple times makes the internals simpler._
   */
  abstract fun render(
    input: InputT,
    state: StateT,
    context: RenderContext<StateT, OutputT>
  ): RenderingT

  /**
   * Called whenever the state changes to generate a new [Snapshot] of the state.
   *
   * **Snapshots must be lazy.**
   *
   * Serialization must not be done at the time this method is called,
   * since the state will be snapshotted frequently but the serialized form may only be needed very
   * rarely.
   *
   * If the workflow does not have any state, or should always be started from scratch, return
   * [Snapshot.EMPTY] from this method.
   *
   * @see initialState
   */
  abstract fun snapshotState(state: StateT): Snapshot

  /**
   * Satisfies the [Workflow] interface by returning `this`.
   */
  final override fun asStatefulWorkflow(): StatefulWorkflow<InputT, StateT, OutputT, RenderingT> =
    this
}
