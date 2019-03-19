package com.squareup.workflow

/**
 * TODO kdoc
 */
interface WorkflowDebugger {

  /**
   * TODO kdoc
   */
  fun onInitialState(hierarchyInfo: WorkflowHierarchyDebugSnapshot)

  /**
   * TODO kdoc
   */
  fun onUpdate(
    hierarchyInfo: WorkflowHierarchyDebugSnapshot,
    updateInfo: WorkflowUpdateDebugInfo
  )

  object NullDebugger : WorkflowDebugger {
    override fun onInitialState(hierarchyInfo: WorkflowHierarchyDebugSnapshot) {
    }

    override fun onUpdate(
      hierarchyInfo: WorkflowHierarchyDebugSnapshot,
      updateInfo: WorkflowUpdateDebugInfo
    ) {
    }
  }

  object StdoutDebugger : WorkflowDebugger {

    override fun onInitialState(hierarchyInfo: WorkflowHierarchyDebugSnapshot) {
      println("initial state:\n$hierarchyInfo")
    }

    override fun onUpdate(
      hierarchyInfo: WorkflowHierarchyDebugSnapshot,
      updateInfo: WorkflowUpdateDebugInfo
    ) {
      println("update:\n$hierarchyInfo\n$updateInfo")
    }
  }
}

/**
 * TODO kdoc
 */
data class WorkflowUpdateDebugInfo(
  val workflowType: String,
  val kind: Kind
) {
  sealed class Kind {
    data class DidUpdate(val source: Source) : Kind() {
      sealed class Source {
        object External : Source() {
          override fun toString(): String = "External"
        }

        object Subscription : Source() {
          override fun toString(): String = "Subscription"
        }

        // TODO isn't this redundant with ChildDidUpdate?
        data class Subtree(val info: WorkflowUpdateDebugInfo) : Source()
      }
    }

    data class ChildDidUpdate(val childInfo: WorkflowUpdateDebugInfo) : Kind()
  }
}

/**
 * TODO kdoc
 */
data class WorkflowHierarchyDebugSnapshot(
  val workflowType: String,
  val stateDescription: String,
  val children: List<Child>
) {
  data class Child(
    val key: String,
    val snapshot: WorkflowHierarchyDebugSnapshot
  )
}
