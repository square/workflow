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
package com.squareup.workflow.internal

import com.squareup.workflow.Snapshot
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.selects.SelectBuilder
import okio.ByteString
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Responsible for tracking child workflows, starting them and tearing them down when necessary.
 * Also manages restoring children from snapshots.
 *
 * Child workflows are stored in [WorkflowChildNode]s, which associate the child's [WorkflowNode]
 * with its output handler.
 *
 * ## Rendering
 *
 * This class implements [RealRenderContext.Renderer], and [WorkflowNode] will pass its instance
 * of this class to the [RealRenderContext] on each render pass to render children. That means that
 * when a workflow renders a child, this class does the actual work.
 *
 * This class keeps two lists:
 *  1. Active list: All the children from the last render pass that have not yet been rendered in
 *     the subsequent pass.
 *  2. Staging list: Children that have been rendered in the current render pass, before the pass is
 *     [committed][commitRenderedChildren].
 *
 * The render process is as follows:
 *   1. When the render pass starts, the staging list is empty and the active list contains all the
 *      children rendered in the last pass.
 *      ```
 *      active:  [foo, bar]
 *      staging: []
 *      ```
 *   2. Every time a child is rendered, it is looked up in the list of children from the last render
 *      pass. If found, it is removed from the active list and added to the staging list.
 *      ```
 *      render(foo)
 *      active:  [bar]
 *      staging: [foo]
 *      ```
 *   3. If not found, a new [WorkflowChildNode] is created and added to the staging list.
 *      ```
 *      render(baz)
 *      active:  [bar]
 *      staging: [foo, baz]
 *      ```
 *   4. When the workflow's render method returns, the [WorkflowNode] calls
 *      [commitRenderedChildren], which:
 *        1. Tears down all the children remaining in the active list
 *           ```
 *           bar.cancel()
 *           active:  [bar]
 *           staging: [foo, baz]
 *           ```
 *        2. Clears the old active list
 *           ```
 *           active:  []
 *           staging: [foo, baz]
 *           ```
 *        3. And then swaps the active and staging lists.
 *           ```
 *           active:  [foo, baz]
 *           staging: []
 *           ```
 *      This just updates a couple references, and since the lists are swapped, doesn't involve any
 *      allocations.
 *
 * When looking up a child in the active list, a linear search is used. This is expected to perform
 * adequately in practice because most workflows don't have a large number of children (even as few
 * as ten is uncommon), and in the most common case, the structure of the workflow tree doesn't
 * change (no workflows are added or removed), and children are re-rendered in the same order as
 * before, so the first active child will usually match.
 */
internal class SubtreeManager<StateT, OutputT : Any>(
  private val contextForChildren: CoroutineContext,
  private val emitActionToParent: (WorkflowAction<StateT, OutputT>) -> Any?,
  private val parentDiagnosticId: Long,
  private val diagnosticListener: WorkflowDiagnosticListener? = null,
  private val idCounter: IdCounter? = null,
  private val workerContext: CoroutineContext = EmptyCoroutineContext
) : RealRenderContext.Renderer<StateT, OutputT> {

  /**
   * When this manager's node is restored from a snapshot, its children snapshots are extracted into
   * this cache. Then, when those children are started for the first time, they are also restored
   * from their snapshots.
   */
  private val snapshotCache = mutableMapOf<AnyId, ByteString>()

  private var children = ActiveStagingList<WorkflowChildNode<*, *, *, *>>()

  /**
   * Moves all the nodes that have been accumulated in the staging list to the active list, making
   * them the new active list, and tears down any inactive children.
   *
   * This should be called after this node's render method returns.
   */
  fun commitRenderedChildren() {
    // Any children left in the previous active list after the render finishes were not re-rendered
    // and must be torn down.
    children.commitStaging { child ->
      child.workflowNode.cancel()
      snapshotCache -= child.id
    }
  }

  /* ktlint-disable parameter-list-wrapping */
  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> render(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT {
    /* ktlint-enable parameter-list-wrapping */

    // Prevent duplicate workflows with the same key.
    children.forEachStaging {
      require(!(it.matches(child, key))) {
        "Expected keys to be unique for ${child::class.java.name}: key=$key"
      }
    }

    // Start tracking this case so we can be ready to render it.
    val stagedChild = children.retainOrCreate(
        predicate = { it.matches(child, key) },
        create = { createChildNode(child, props, key, handler) }
    )
    stagedChild.setHandler(handler)
    return stagedChild.render(child.asStatefulWorkflow(), props)
  }

  /**
   * Uses [selector] to invoke [WorkflowNode.tick] for every running child workflow this instance
   * is managing.
   */
  fun <T : Any> tickChildren(selector: SelectBuilder<T?>) {
    children.forEachActive { child ->
      child.workflowNode.tick(selector)
    }
  }

  fun createChildSnapshots(): List<Pair<AnyId, Snapshot>> {
    val snapshots = mutableListOf<Pair<AnyId, Snapshot>>()
    children.forEachActive { child ->
      val snapshot = child.workflowNode.snapshot(child.workflow.asStatefulWorkflow())
      snapshots += child.id to snapshot
    }
    return snapshots
  }

  /**
   * Caches snapshots and IDs from a tree snapshot so the next time those workflows are asked to be
   * started, they are restored from their snapshots.
   */
  fun restoreChildrenFromSnapshots(childSnapshots: List<Pair<AnyId, ByteString>>) {
    snapshotCache.putAll(childSnapshots.toMap())
  }

  private fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> createChildNode(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    initialProps: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): WorkflowChildNode<ChildPropsT, ChildOutputT, StateT, OutputT> {
    val id = child.id(key)
    lateinit var node: WorkflowChildNode<ChildPropsT, ChildOutputT, StateT, OutputT>

    fun acceptChildOutput(output: ChildOutputT): Any? {
      val action = node.acceptChildOutput(output)
      return emitActionToParent(action)
    }

    val workflowNode = WorkflowNode(
        id,
        child.asStatefulWorkflow(),
        initialProps,
        snapshotCache[id],
        contextForChildren,
        ::acceptChildOutput,
        parentDiagnosticId,
        diagnosticListener,
        idCounter,
        workerContext = workerContext
    )
    return WorkflowChildNode(child, handler, workflowNode)
        .also { node = it }
  }
}
