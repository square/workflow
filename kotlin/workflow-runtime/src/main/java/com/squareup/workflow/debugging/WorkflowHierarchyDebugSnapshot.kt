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
package com.squareup.workflow.debugging

import com.squareup.workflow.debugging.WorkflowHierarchyDebugSnapshot.ChildWorkflow
import com.squareup.workflow.internal.WorkflowId
import java.util.Objects
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Describes a tree of snapshots of the states of the entire workflow tree after a particular render
 * pass.
 *
 * Emitted from [com.squareup.workflow.WorkflowSession.debugInfo].
 *
 * @param workflowType A string representation of the type of this workflow.
 * @param state The actual state value of the workflow.
 * @param children All the child workflows that were rendered by this workflow in the last render
 * pass. See [ChildWorkflow].
 */
data class WorkflowHierarchyDebugSnapshot(
  val workflowType: String,
  val props: Any?,
  val state: Any?,
  val rendering: Any?,
  val children: List<ChildWorkflow>,
  val workers: List<ChildWorker>
) {
  /** Convenience constructor to get the workflow type string the right way. */
  constructor(
    workflowId: WorkflowId<*, *, *>,
    props: Any?,
    state: Any?,
    rendering: Any?,
    children: List<ChildWorkflow>,
    workers: List<ChildWorker>
  ) : this(workflowId.typeDebugString, props, state, rendering, children, workers)

  /**
   * Represents a workflow that was rendered by the workflow represented by a particular
   * [WorkflowHierarchyDebugSnapshot].
   *
   * @param key The string key that was used to render this child.
   * @param snapshot The [WorkflowHierarchyDebugSnapshot] that describes the state of this child.
   */
  data class ChildWorkflow(
    val key: String,
    val snapshot: WorkflowHierarchyDebugSnapshot
  )

  /**
   * Represents a worker that is being run by the workflow represented by a particular
   * [WorkflowHierarchyDebugSnapshot].
   *
   * @param key The string key that was used to render this child.
   * @param describe A function that returns a string that describes the worker. Includes the output
   * of the worker's `toString` method.
   */
  class ChildWorker(
    val key: String,
    describe: () -> String
  ) {
    val description by lazy(PUBLICATION) { describe() }

    override fun toString(): String = "ChildWorker(key=$key, description=$description)"
    override fun hashCode(): Int = Objects.hash(key, description)
    override fun equals(other: Any?): Boolean =
      other is ChildWorker &&
          key == other.key &&
          description == other.description
  }

  /**
   * This formatted string is expensive to generate, so cache it.
   */
  private val lazyDescription by lazy(PUBLICATION) {
    buildString {
      writeSnapshot(this@WorkflowHierarchyDebugSnapshot)
    }
  }

  override fun toString(): String = """
    |WorkflowHierarchyDebugSnapshot(
    ${toDescriptionString().trimEnd().prependIndent("|  ")}
    |)
  """.trimMargin("|")

  /**
   * Generates a multi-line, recursive string describing the update.
   */
  fun toDescriptionString(): String = lazyDescription
}

private fun StringBuilder.writeSnapshot(snapshot: WorkflowHierarchyDebugSnapshot) {
  append("workflowType: ")
  appendln(snapshot.workflowType)

  append("props: ")
  appendln(snapshot.props)

  append("state: ")
  appendln(snapshot.state)

  append("rendering: ")
  append(snapshot.rendering)

  if (snapshot.children.isNotEmpty()) {
    appendln()
    append("children (")
    append(snapshot.children.size)
    appendln("):")

    for ((index, child) in snapshot.children.withIndex()) {
      append("| key: ")
      appendln(child.key.ifEmpty { "{no key}" })
      append(
          child.snapshot.toDescriptionString()
              .prependIndent("|   ")
      )
      if (index < snapshot.children.size - 1) appendln()
    }
  }

  if (snapshot.workers.isNotEmpty()) {
    appendln()
    append("workers (")
    append(snapshot.workers.size)
    appendln("):")

    for ((index, worker) in snapshot.workers.withIndex()) {
      append("| [")
      append(worker.key.ifEmpty { "{no key}" })
      append("] ")
      append(worker.description)
      if (index < snapshot.workers.size - 1) appendln()
    }
  }
}
