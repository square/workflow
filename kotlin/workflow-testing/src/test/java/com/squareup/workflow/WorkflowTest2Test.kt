package com.squareup.workflow

import kotlin.test.Test

class WorkflowTest2Test {

  @Test fun stuff() {
    testWorkflow { context ->
      TODO("What should this block look like?")
    }
  }
}

private fun testWorkflow(
  block: (WorkflowContext<Nothing, Nothing>) -> Unit
) {

}

private class WUT : Workflow<String, String, String, String> {
  override fun initialState(input: String): String {
    TODO()
  }

  override fun compose(
    input: String,
    state: String,
    context: WorkflowContext<String, String>
  ): String {
    TODO()
  }

  override fun snapshotState(state: String): Snapshot {
    TODO()
  }

  override fun restoreState(snapshot: Snapshot): String {
    TODO()
  }
}
