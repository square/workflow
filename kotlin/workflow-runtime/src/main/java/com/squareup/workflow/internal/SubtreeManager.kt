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
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.diagnostic.IdCounter
import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import kotlinx.coroutines.selects.SelectBuilder
import okio.ByteString
import kotlin.coroutines.CoroutineContext

/**
 * Responsible for tracking child workflows, starting them and tearing them down when necessary.
 * Also manages restoring children from snapshots.
 */
internal class SubtreeManager<StateT, OutputT : Any>(
  private val contextForChildren: CoroutineContext,
  private val parentDiagnosticId: Long,
  private val diagnosticListener: WorkflowDiagnosticListener? = null,
  private val idCounter: IdCounter? = null
) : RealRenderContext.Renderer<StateT, OutputT> {

  /**
   * When this manager's node is restored from a snapshot, its children snapshots are extracted into
   * this cache. Then, when those children are started for the first time, they are also restored
   * from their snapshots.
   */
  private val snapshotCache = mutableMapOf<AnyId, ByteString>()

  private val nodeLifetimeTracker =
    LifetimeTracker<WorkflowOutputCase<*, *, StateT, OutputT>, AnyId, WorkflowNode<*, *, *, *>>(
        getKey = { it.id },
        start = { it.createNode() },
        dispose = { case, node ->
          node.cancel()
          snapshotCache -= case.id
        }
    )

  // @formatter:off
  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT>
      render(
        case: WorkflowOutputCase<ChildPropsT, ChildOutputT, StateT, OutputT>,
        child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
        id: WorkflowId<ChildPropsT, ChildOutputT, ChildRenderingT>,
        props: ChildPropsT
      ): ChildRenderingT {
  // @formatter:on
    // Start tracking this case so we can be ready to render it.
    @Suppress("UNCHECKED_CAST")
    val childNode = nodeLifetimeTracker.ensure(case) as
        WorkflowNode<ChildPropsT, *, ChildOutputT, ChildRenderingT>
    return childNode.render(child.asStatefulWorkflow(), props)
  }

  /**
   * Ensures that a [WorkflowNode] is running for every workflow in [cases], and cancels any
   * running workflows that are not in the list.
   */
  fun track(cases: List<WorkflowOutputCase<*, *, StateT, OutputT>>) {
    // Add new children and remove old ones.
    nodeLifetimeTracker.track(cases)
  }

  /**
   * Uses [selector] to invoke [WorkflowNode.tick] for every running child workflow this instance
   * is managing.
   */
  fun <T : Any> tickChildren(
    selector: SelectBuilder<T?>,
    handler: (WorkflowAction<StateT, OutputT>?) -> T?
  ) {
    for ((case, node) in nodeLifetimeTracker.lifetimes) {
      node.tick(selector) { output ->
        if (output != null) {
          val componentUpdate = case.acceptChildOutput(output)
          return@tick handler(componentUpdate)
        } else return@tick handler(null)
      }
    }
  }

  fun createChildSnapshots(): List<Pair<AnyId, Snapshot>> {
    return nodeLifetimeTracker.lifetimes
        .map { (case, node) -> node.id to node.snapshot(case.workflow.asStatefulWorkflow()) }
  }

  /**
   * Caches snapshots and IDs from a tree snapshot so the next time those workflows are asked to be
   * started, they are restored from their snapshots.
   */
  fun restoreChildrenFromSnapshots(childSnapshots: List<Pair<AnyId, ByteString>>) {
    snapshotCache.putAll(childSnapshots.toMap())
  }

  @Suppress("UNCHECKED_CAST")
  private fun <IC, OC : Any> WorkflowOutputCase<IC, OC, StateT, OutputT>.createNode():
      WorkflowNode<IC, *, OC, *> =
    WorkflowNode(
        id,
        workflow.asStatefulWorkflow() as StatefulWorkflow<IC, *, OC, *>,
        props,
        snapshotCache[id],
        contextForChildren,
        parentDiagnosticId,
        diagnosticListener,
        idCounter
    )
}
