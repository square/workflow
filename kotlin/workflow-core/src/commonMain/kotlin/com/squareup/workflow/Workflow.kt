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
 * A composable, optionally-stateful object that can [handle events][RenderContext.onEvent],
 * [delegate to children][RenderContext.renderChild], [subscribe][RenderContext.onWorkerOutput] to
 * arbitrary asynchronous events from the outside world.
 *
 * The basic purpose of a `Workflow` is to take some input (in the form of [PropsT]) and
 * return a [rendering][RenderingT]. To that end, a workflow may keep track of internal
 * [state][StatefulWorkflow], recursively ask other workflows to render themselves, subscribe to
 * data streams from the outside world, and handle events both from its
 * [renderings][RenderContext.onEvent] and from workflows it's delegated to (its "children"). A
 * `Workflow` may also emit [output events][OutputT] up to its parent `Workflow`.
 *
 * Workflows form a tree, where each workflow can have zero or more child workflows. Child workflows
 * are started as necessary whenever another workflow asks for them, and are cleaned up
 * automatically when they're no longer needed. [Props][PropsT] propagates down the tree,
 * [outputs][OutputT] and [renderings][RenderingT] propagate up the tree.
 *
 * ## Implementing `Workflow`
 *
 * The [Workflow] interface is useful as a facade for your API. You can publish an interface that
 * extends `Workflow`, and keep the implementation (e.g. is your workflow state*ful* or
 * state*less* a private implementation detail.
 *
 * ### [Stateful Workflows][StatefulWorkflow]
 *
 * If your workflow needs to keep track of internal state, implement the [StatefulWorkflow]
 * interface. That interface has an additional type parameter, `StateT`, and allows you to specify
 * [how to create the initial state][StatefulWorkflow.initialState] and how to
 * [snapshot][StatefulWorkflow.snapshotState]/restore your state.
 *
 * ### [Stateless Workflows][StatelessWorkflow]
 *
 * If your workflow simply needs to delegate to other workflows, maybe transforming propss, outputs,
 * or renderings, extend [StatelessWorkflow], or just pass a lambda to the [stateless] function
 * below.
 *
 * ## Interacting with Events and Other Workflows
 *
 * All workflows are passed a [RenderContext] in their render methods. This context allows the
 * workflow to interact with the outside world by doing things like listening for events,
 * subscribing to streams of data, rendering child workflows, and performing cleanup when the
 * workflow is about to be torn down by its parent. See the documentation on [RenderContext] for
 * more information about what it can do.
 *
 * @param PropsT Typically a data class that is used to pass configuration information or bits of
 * state that the workflow can always get from its parent and needn't duplicate in its own state.
 * May be [Unit] if the workflow does not need any props data.
 *
 * @param OutputT Typically a sealed class that represents "events" that this workflow can send
 * to its parent.
 * May be [Nothing] if the workflow doesn't need to emit anything.
 *
 * @param RenderingT The value returned to this workflow's parent during [composition][renderChild].
 * Typically represents a "view" of this workflow's props, current state, and children's renderings.
 * A workflow that represents a UI component may use a view model as its rendering type.
 *
 * @see StatefulWorkflow
 * @see StatelessWorkflow
 */
interface Workflow<in PropsT, out OutputT : Any, out RenderingT> {

  /**
   * Provides a [StatefulWorkflow] view of this workflow. Necessary because [StatefulWorkflow] is
   * the common API required for [RenderContext.renderChild] to do its work.
   */
  fun asStatefulWorkflow(): StatefulWorkflow<PropsT, *, OutputT, RenderingT>

  /**
   * Empty companion serves as a hook point to allow us to create `Workflow.foo`
   * extension methods elsewhere.
   */
  companion object
}
