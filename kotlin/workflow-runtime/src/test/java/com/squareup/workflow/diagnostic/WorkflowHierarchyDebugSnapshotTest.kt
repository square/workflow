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
package com.squareup.workflow.diagnostic

import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorker
import com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot.ChildWorkflow
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkflowHierarchyDebugSnapshotTest {

  @Test fun `fancy toDescriptionString`() {
    val snapshot = WorkflowHierarchyDebugSnapshot(
        "root type",
        "root props",
        "root state",
        "root rendering",
        children = listOf(
            ChildWorkflow(
                "first child",
                WorkflowHierarchyDebugSnapshot(
                    "first child type",
                    "first child props",
                    "first child state",
                    "first child rendering",
                    children = listOf(
                        ChildWorkflow(
                            "",
                            WorkflowHierarchyDebugSnapshot(
                                "nested child type",
                                "nested child props",
                                "nested child state",
                                "nested child rendering",
                                children = emptyList(),
                                workers = emptyList()
                            )
                        )
                    ),
                    workers = emptyList()
                )
            ),
            ChildWorkflow(
                "second child",
                WorkflowHierarchyDebugSnapshot(
                    "second child type",
                    "second child props",
                    "second child state",
                    "second child rendering",
                    children = emptyList(),
                    workers = emptyList()
                )
            )
        ),
        workers = listOf(
            ChildWorker("first worker key", "first worker description"),
            ChildWorker("", "second worker description")
        )
    )
    val expected = """
      workflowType: root type
      props: root props
      state: root state
      rendering: root rendering
      children (2):
      | key: first child
      |   workflowType: first child type
      |   props: first child props
      |   state: first child state
      |   rendering: first child rendering
      |   children (1):
      |   | key: {no key}
      |   |   workflowType: nested child type
      |   |   props: nested child props
      |   |   state: nested child state
      |   |   rendering: nested child rendering
      | key: second child
      |   workflowType: second child type
      |   props: second child props
      |   state: second child state
      |   rendering: second child rendering
      workers (2):
      | [first worker key] first worker description
      | [{no key}] second worker description
    """.trimIndent()

    assertEquals(expected, snapshot.toDescriptionString())
  }

  @Test fun `fancy toString`() {
    val snapshot = WorkflowHierarchyDebugSnapshot(
        "root type",
        "root props",
        "root state",
        "root rendering",
        children = listOf(
            ChildWorkflow(
                "second child",
                WorkflowHierarchyDebugSnapshot(
                    "second child type",
                    "second child props",
                    "second child state",
                    "second child rendering",
                    children = emptyList(),
                    workers = emptyList()
                )
            )
        ),
        workers = listOf(
            ChildWorker("key", "description")
        )
    )
    val formatted = snapshot.toString()
    val expected = """
      WorkflowHierarchyDebugSnapshot(
        workflowType: root type
        props: root props
        state: root state
        rendering: root rendering
        children (1):
        | key: second child
        |   workflowType: second child type
        |   props: second child props
        |   state: second child state
        |   rendering: second child rendering
        workers (1):
        | [key] description
      )
    """.trimIndent()

    assertEquals(expected, formatted)
  }
}
