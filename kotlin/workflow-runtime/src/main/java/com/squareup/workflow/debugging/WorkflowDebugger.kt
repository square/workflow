package com.squareup.workflow.debugging

import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

typealias WorkflowDebuggerInitializer = (
  debugger: WorkflowDebugger,
  workflowScope: CoroutineScope,
  type: String
) -> Unit

/**
 * TODO write documentation
 */
interface WorkflowDebugger {

  /**
   * When true, [trees] will always emit after every render pass, even if [breakpointEnabled] is
   * false.
   */
  var emitTrees: Boolean

  /**
   * When true, the workflow runtime will pause after every render pass, and [resume] must be called
   * before any actions will be processed.
   *
   * After pausing, [trees] will emit the latest [WorkflowHierarchyDebugSnapshot] (even if
   * [emitTrees] is false).
   *
   * If the runtime is paused, clearing this flag will not resume it – [resume] must be called
   * explicitly.
   */
  var breakpointEnabled: Boolean

  /**
   * When either [emitTrees] [breakpointEnabled] is true, emits a [WorkflowHierarchyDebugSnapshot]
   * after every render pass that describes the states of all active workflows.
   *
   * This flow replays: collectors who begin collecting this flow after the runtime have started
   * will immediately get the tree from the last render pass.
   */
  val trees: Flow<WorkflowHierarchyDebugSnapshot>

  /**
   * If the runtime is currently paused (when [breakpointEnabled] is true after a render pass),
   * this method must be called to continue processing actions.
   *
   * If the runtime is not paused, calling this method does nothing.
   */
  fun resume()

  /**
   * Adds a [WorkflowDiagnosticListener] that will get notified when events occur.
   *
   * It is an error to add a listener after the runtime has started.
   *
   * @throws IllegalStateException If called after the runtime has started.
   */
  fun addDiagnosticListener(listener: WorkflowDiagnosticListener)
}
