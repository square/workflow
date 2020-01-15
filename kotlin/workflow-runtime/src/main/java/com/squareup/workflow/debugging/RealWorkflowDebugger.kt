package com.squareup.workflow.debugging

import com.squareup.workflow.diagnostic.WorkflowDiagnosticListener
import com.squareup.workflow.diagnostic.andThen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

internal class RealWorkflowDebugger : WorkflowDebugger {

  @Volatile override var emitTrees: Boolean = false

  @Volatile override var breakpointEnabled: Boolean = false

  var diagnosticListener: WorkflowDiagnosticListener? = null
    private set

  private var isRunning = false

  private val resumeSignal = Channel<Unit>(capacity = 1)

  @UseExperimental(ExperimentalCoroutinesApi::class)
  private val _trees = ConflatedBroadcastChannel<WorkflowHierarchyDebugSnapshot>()

  @UseExperimental(FlowPreview::class)
  override val trees: Flow<WorkflowHierarchyDebugSnapshot>
    get() = _trees.asFlow()

  override fun resume() {
    resumeSignal.offer(Unit)
  }

  override fun addDiagnosticListener(listener: WorkflowDiagnosticListener) {
    require(!isRunning)
    diagnosticListener = diagnosticListener?.andThen(listener) ?: listener
  }

  /**
   * Freezes the debugger so no more listeners can be added.
   */
  fun onRuntimeStarted() {
    isRunning = true
  }

  /**
   * Should be called after each render pass to generate and emit the new tree and/or suspend for
   * breakpoint, if necessary.
   *
   * This is an inline function so the compiler doesn't need to generate an additional nested state
   * machine.
   */
  suspend inline fun maybeEmitOrBreak(root: WorkflowTreeFactory) {
    val doBreak = breakpointEnabled
    // Clear stale resume requests before emitting the new tree, in case a collector immediately
    // requests a resume.
    if (doBreak) clearResume()
    if (doBreak || emitTrees) emitTree(root)
    if (doBreak) suspendForBreak()
  }

  /**
   * Clears any stale resume requests.
   */
  fun clearResume() {
    resumeSignal.poll()
  }

  suspend fun suspendForBreak() {
    resumeSignal.receive()
  }

  private fun emitTree(root: WorkflowTreeFactory) {
    val tree = root.buildDebugSnapshot()
    @UseExperimental(ExperimentalCoroutinesApi::class)
    _trees.offer(tree)
  }
}

internal fun WorkflowDebuggerInitializer.createDebugger(
  workflowScope: CoroutineScope,
  type: String
): RealWorkflowDebugger =
  RealWorkflowDebugger().also { debugger ->
    this.invoke(debugger, workflowScope, type)
    debugger.onRuntimeStarted()
  }
