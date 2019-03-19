package com.squareup.workflow.internal

import com.squareup.workflow.WorkflowUpdateDebugInfo

data class WorkflowOutput<out O>(
  val value: O,
  val debugInfo: WorkflowUpdateDebugInfo
)
