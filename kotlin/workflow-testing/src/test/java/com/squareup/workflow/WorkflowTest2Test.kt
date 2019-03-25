package com.squareup.workflow

import kotlin.test.Test

class WorkflowTest2Test {

  @Test fun stuff() {
    val wut = WUT()
    testWorkflow { context ->
      TODO("What should this block look like?")
      context.assertCompose(wut, "input")
    }
  }
}

private interface TestWorkflowContext {

  fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any> assertCompose(
    child: Workflow<InputT, StateT, OutputT, RenderingT>,
    input: InputT
  ): ComposeAssertion<RenderingT>
}

private interface ComposeAssertion<RenderingT : Any> {


}

private fun testWorkflow(
  block: (TestWorkflowContext) -> Unit
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
