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

import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Passthrough
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Sink
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Subtree
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Worker
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

@UseExperimental(UnstableDefault::class)
class WorkflowUpdateDebugInfoTest {

  private val passthroughUpdate = WorkflowUpdateDebugInfo(
      "root type",
      Passthrough(
          "key",
          WorkflowUpdateDebugInfo(
              "child type",
              Updated(Sink)
          )
      )
  )

  private val workerUpdate = WorkflowUpdateDebugInfo(
      "root type",
      Updated(Worker("key", "output"))
  )

  private val subtreeUpdate = WorkflowUpdateDebugInfo(
      "root type",
      Updated(
          Subtree(
              "key", "output",
              WorkflowUpdateDebugInfo(
                  "child type",
                  Updated(Sink)
              )
          )
      )
  )

  private val json = Json(
      configuration = JsonConfiguration(prettyPrint = true),
      context = WorkflowUpdateDebugInfo.serializationModule
  )

  @Test fun `passthrough toDescriptionString`() {
    val expected = """
      root type passing through from workflow[key=key]
      ↳ child type updated from sink
    """.trimIndent()

    assertEquals(expected, passthroughUpdate.toDescriptionString())
  }

  @Test fun `updated worker toDescriptionString`() {
    val expected = """
      root type updated from worker[key=key]: output
    """.trimIndent()

    assertEquals(expected, workerUpdate.toDescriptionString())
  }

  @Test fun `updated subtree toDescriptionString`() {
    val expected = """
      root type updated from workflow[key=key]: output
      ↳ child type updated from sink
    """.trimIndent()

    assertEquals(expected, subtreeUpdate.toDescriptionString())
  }

  @Test fun `passthrough serialization`() {
    val actual = json.stringify(WorkflowUpdateDebugInfo.serializer(), passthroughUpdate)
    val expected = """
      {
          "workflowType": "root type",
          "kind": {
              "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Passthrough",
              "key": "key",
              "childInfo": {
                  "workflowType": "child type",
                  "kind": {
                      "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated",
                      "source": {
                          "type": "kotlin.collections.LinkedHashMap"
                      }
                  }
              }
          }
      }
    """.trimIndent()

    assertEquals(expected, actual)
  }

  @Test fun `updated worker serialization`() {
    val actual = json.stringify(WorkflowUpdateDebugInfo.serializer(), workerUpdate)
    val expected = """
      {
          "workflowType": "root type",
          "kind": {
              "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated",
              "source": {
                  "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Worker",
                  "key": "key",
                  "output": "output"
              }
          }
      }
    """.trimIndent()

    assertEquals(expected, actual)
  }

  @Test fun `updated subtree serialization`() {
    val actual = json.stringify(WorkflowUpdateDebugInfo.serializer(), subtreeUpdate)
    val expected = """
      {
          "workflowType": "root type",
          "kind": {
              "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated",
              "source": {
                  "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Subtree",
                  "key": "key",
                  "output": "output",
                  "childInfo": {
                      "workflowType": "child type",
                      "kind": {
                          "type": "com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated",
                          "source": {
                              "type": "kotlin.collections.LinkedHashMap"
                          }
                      }
                  }
              }
          }
      }
    """.trimIndent()

    assertEquals(expected, actual)
  }

}
