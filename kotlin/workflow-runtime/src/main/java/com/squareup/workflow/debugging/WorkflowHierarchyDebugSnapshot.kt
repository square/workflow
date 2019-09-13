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

import com.squareup.workflow.debugging.WorkflowHierarchyDebugSnapshot.Child
import com.squareup.workflow.internal.WorkflowId
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
 * pass. See [Child].
 */
data class WorkflowHierarchyDebugSnapshot(
  val workflowType: String,
  val props: Any?,
  val state: Any?,
  val rendering: Any?,
  val children: List<Child>
) {
  /** Convenience constructor to get the workflow type string the right way. */
  constructor(
    workflowId: WorkflowId<*, *, *>,
    props: Any?,
    state: Any?,
    rendering: Any?,
    children: List<Child>
  ) : this(workflowId.typeDebugString, props, state, rendering, children)

  /**
   * Represents a workflow that was rendered by the workflow represented by a particular
   * [WorkflowHierarchyDebugSnapshot].
   *
   * @param key The string key that was used to render this child.
   * @param snapshot The [WorkflowHierarchyDebugSnapshot] that describes the state of this child.
   */
  data class Child(
    val key: String,
    val snapshot: WorkflowHierarchyDebugSnapshot
  )

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
    appendln("children (${snapshot.children.size}):")

    val childIndent = "|   "
    for ((index, child) in snapshot.children.withIndex()) {
      append("| key: ")
      appendln(child.key.ifEmpty { "{no key}" })
      append(
          child.snapshot.toDescriptionString()
              .prependIndent(childIndent)
      )
      if (index < snapshot.children.size - 1) appendln()
    }
  }
}
