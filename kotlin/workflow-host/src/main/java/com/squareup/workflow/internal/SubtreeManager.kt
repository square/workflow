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
import com.squareup.workflow.WorkflowHierarchyDebugSnapshot
import com.squareup.workflow.WorkflowUpdateDebugInfo
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.parse
import com.squareup.workflow.readByteStringWithLength
import com.squareup.workflow.writeByteStringWithLength
import kotlinx.coroutines.experimental.selects.SelectBuilder
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Responsible for tracking child workflows, starting them and tearing them down when necessary. Also
 * manages restoring children from snapshots.
 */
internal class SubtreeManager<StateT : Any, OutputT : Any>(
  private val contextForChildren: CoroutineContext
) : RealWorkflowContext.Composer<StateT, OutputT> {

  /**
   * When this manager's host is restored from a snapshot, its children snapshots are extracted into
   * this cache. Then, when those children are started for the first time, they are also restored
   * from their snapshots.
   */
  private val snapshotCache = mutableMapOf<AnyId, Snapshot>()

  private val hostLifetimeTracker =
    LifetimeTracker<WorkflowOutputCase<*, *, StateT, OutputT>, AnyId, WorkflowNode<*, *, *, *>>(
        getKey = { it.id },
        start = { it.createNode() },
        dispose = { case, host ->
          host.cancel()
          snapshotCache -= case.id
        }
    )

  // @formatter:off
  override fun <ChildInputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any>
      compose(
        case: WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT>,
        child: Workflow<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
        id: WorkflowId<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
        input: ChildInputT
      ): Pair<ChildRenderingT, WorkflowHierarchyDebugSnapshot> {
  // @formatter:on
    // Start tracking this case so we can be ready to compose it.
    @Suppress("UNCHECKED_CAST")
    val host = hostLifetimeTracker.ensure(case) as
        WorkflowNode<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>
    return host.compose(child, input)
  }

  /**
   * Ensures that a [WorkflowNode] is running for every workflow in [cases], and cancels any
   * running workflows that are not in the list.
   */
  fun track(cases: List<WorkflowOutputCase<*, *, StateT, OutputT>>) {
    // Add new children and remove old ones.
    hostLifetimeTracker.track(cases)
  }

  /**
   * Uses [selector] to invoke [WorkflowNode.tick] for every running child workflow this instance
   * is managing.
   */
  fun <T : Any> tickChildren(
    selector: SelectBuilder<WorkflowOutput<T?>>,
    handler: (WorkflowAction<StateT, OutputT>, WorkflowUpdateDebugInfo) -> WorkflowOutput<T?>
  ) {
    for ((case, host) in hostLifetimeTracker.lifetimes) {
      host.tick(selector) { output ->
        val workflowAction = case.acceptChildOutput(output.value)
        return@tick handler(workflowAction, output.debugInfo)
      }
    }
  }

  /**
   * Returns a [Snapshot] that contains snapshots of all children, associated with their IDs.
   */
  fun createChildrenSnapshot(): Snapshot {
    return Snapshot.write { sink ->
      val childSnapshots = hostLifetimeTracker.lifetimes
          .entries
          .map { (case, host) -> host.id to host.snapshot(case.workflow) }
      sink.writeInt(childSnapshots.size)
      for ((id, snapshot) in childSnapshots) {
        sink.writeByteStringWithLength(id.toByteString())
        sink.writeByteStringWithLength(snapshot.bytes)
      }
    }
  }

  /**
   * Extracts child snapshots and IDs from [snapshot] and caches them, so the next time those state
   * machines are asked to be started, they are restored from their snapshots.
   */
  fun restoreChildrenFromSnapshot(snapshot: Snapshot) {
    snapshot.bytes.parse { source ->
      val childCount = source.readInt()
      val snapshots = List(childCount) {
        val id = restoreId(source.readByteStringWithLength())
        val childSnapshot = source.readByteStringWithLength()
        Pair(id, Snapshot.of(childSnapshot))
      }
      // Populate the snapshot cache so when we are asked to create hosts for the snapshotted IDs,
      // they will be restored from their snapshots.
      snapshotCache.putAll(snapshots.toMap())
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <IC : Any, OC : Any> WorkflowOutputCase<IC, OC, StateT, OutputT>.createNode():
      WorkflowNode<IC, *, OC, *> =
    WorkflowNode(
        id as WorkflowId<IC, Any, OC, Any>,
        workflow as Workflow<IC, Any, OC, Any>,
        input,
        snapshotCache[id],
        contextForChildren
    )
}
