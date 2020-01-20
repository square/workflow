package com.squareup.workflow.diagnostic

import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.diagnostic.BrowseableDebugData.Data
import com.squareup.workflow.diagnostic.BrowseableDebugData.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TODO write documentation
 */
@VeryExperimentalWorkflow
class DebugSnapshotServer {

  private val debugSnapshots = ConflatedBroadcastChannel<WorkflowHierarchyDebugSnapshot>()

  private val recordingListener =
    DebugSnapshotRecordingListener { workflowHierarchyDebugSnapshot, _ ->
      debugSnapshots.offer(workflowHierarchyDebugSnapshot)
    }

  val listener = object : WorkflowDiagnosticListener by recordingListener {
    override fun onRuntimeStarted(
      workflowScope: CoroutineScope,
      rootWorkflowType: String
    ) {
      workflowScope.launch {
        serve()
      }
      recordingListener.onRuntimeStarted(workflowScope, rootWorkflowType)
    }
  }

  private val debugRoots: Flow<Node> = debugSnapshots.asFlow()
      .map { it.asDebugData() }

  private suspend fun serve() {
    // Compute debug data off the main thread since the snapshot types are all immutable and
    // thread-safe anyway.
    withContext(Dispatchers.Default) {
      serveDebugData(debugRoots)
    }
  }
}

private fun WorkflowHierarchyDebugSnapshot.asDebugData(): Node = Node(
    children = mapOf(
        "type" to { Data(this.workflowType) },
        "props" to { Data(this.props.toString()) },
        "state" to { Data(this.state.toString()) },
        "rendering" to { Data(this.rendering.toString()) },
        "workers" to {
          Node(children = workers.associate {
            "${it.description}:${it.key}" to { Data("") }
          })
        },
        "workflows" to {
          Node(children = children.associate {
            "${it.snapshot.workflowType}:${it.key}" to it.snapshot::asDebugData
          })
        }
    )
)
