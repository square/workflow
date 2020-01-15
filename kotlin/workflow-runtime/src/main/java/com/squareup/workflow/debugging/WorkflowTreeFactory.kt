package com.squareup.workflow.debugging

/**
 * TODO write documentation
 */
internal interface WorkflowTreeFactory {
  fun buildDebugSnapshot(): WorkflowHierarchyDebugSnapshot
}
