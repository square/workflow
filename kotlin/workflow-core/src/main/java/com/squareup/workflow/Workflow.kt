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
 * A composable, optionally-stateful object that can [handle events][WorkflowContext.onEvent],
 * [delegate to children][WorkflowContext.compose], [subscribe][onReceive] to arbitrary streams from
 * the outside world, and be [saved][snapshotState] to a serialized form to be
 * [restored][restoreState] later.
 *
 * The basic purpose of a `Workflow` is to take some [input][InputT] and return a
 * [rendering][RenderingT]. To that end, a workflow may keep track of internal [state][StateT],
 * recursively ask other workflows to render themselves, subscribe to data streams from the outside
 * world, and handle events both from its [renderings][WorkflowContext.onEvent] and from workflows
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
 * and later used to [restore][restoreState] the workflow. **Implementations of the `Workflow`
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
 * @param RenderingT The value returned to this workflow's parent during [composition][compose].
 * Typically represents a "view" of this workflow's input, current state, and children's renderings.
 * A workflow that represents a UI component may use a view model as its rendering type.
 */
interface Workflow<in InputT : Any, StateT : Any, out OutputT : Any, out RenderingT : Any> {

  /**
   * Called when the state machine is first started to get the initial state.
   */
  fun initialState(input: InputT): StateT

  /**
   * Called whenever [WorkflowContext.compose] is about to get a new [input][InputT] value, to allow
   * the workflow to modify its state in response. This method is called eagerly: `old` and `new` might
   * be the same value, so it is up to implementing code to perform any diffing if desired.
   *
   * Default implementation does nothing.
   */
  fun onInputChanged(
    old: InputT,
    new: InputT,
    state: StateT
  ): StateT = state

  /**
   * Called at least once† any time one of the following things happens:
   *  - This workflow's [input] changes (via the parent passing a different one in).
   *  - This workflow's [state] changes.
   *  - A child workflow's state changes.
   *
   * **Never call this method directly.** To get the rendering from a child workflow, pass the child
   * and any required input to [WorkflowContext.compose].
   *
   * This method *should not* have any side effects, and in particular should not do anything that
   * blocks the current thread. It may be called multiple times for the same state. It must do all its
   * work by calling methods on [context].
   *
   * _† This method is guaranteed to be called *at least* once for every state, but may be called
   * multiple times. Allowing this method to be invoked multiple times makes the internals simpler._
   */
  fun compose(
    input: InputT,
    state: StateT,
    context: WorkflowContext<StateT, OutputT>
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
   * @see restoreState
   */
  fun snapshotState(state: StateT): Snapshot

  /**
   * Deserialize a state value from a [Snapshot] previously created with [snapshotState].
   *
   * If the workflow should always be started from scratch, this method can just ignore the snapshot
   * and return the initial state.
   *
   * @see snapshotState
   */
  fun restoreState(snapshot: Snapshot): StateT
}
