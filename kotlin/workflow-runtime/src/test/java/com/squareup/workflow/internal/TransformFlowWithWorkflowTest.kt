package com.squareup.workflow.internal

import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.stateless
import com.squareup.workflow.transformWithWorkflow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TransformFlowWithWorkflowTest {

  @Test fun `simple transformation`() {
    val flow = flowOf(1, 2, 3)

    val transformed =
      flow.transformWithWorkflow(Workflow.stateless<Worker<Int>, String, Unit> { upstream ->
        runningWorker(upstream) {
          WorkflowAction { it.toString() }
        }
      })

    val result = runBlocking { transformed.toList() }
    assertEquals(listOf("1", "2", "3"), result, "Expected $result to be [1, 2, 3]")
  }
}
