package com.squareup.workflow

import com.squareup.workflow.HarnessState.Running
import com.squareup.workflow.HarnessState.Success
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.noop
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class WorkflowTest2Test {

  @Test fun stuff() = testWorkflow { context ->
    val wut = WUT()
    val rendering = context.compose(wut, "input")
    assertEquals("rendering", rendering)
  }
}

class TestWorkflowContext internal constructor(
  private val workflowContext: WorkflowContext<Unit, Nothing>
) {

  fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any> compose(
    child: Workflow<InputT, StateT, OutputT, RenderingT>,
    input: InputT,
    expectedOutput: (OutputT) -> Unit = { fail("Expected no output, got $it") }
  ): RenderingT = workflowContext.compose(child, input) { output ->
    expectedOutput(output)
    noop()
  }
}

fun testWorkflow(block: (TestWorkflowContext) -> Unit) {
  val hostFactory = WorkflowHost.Factory(Dispatchers.Unconfined)
  val host = hostFactory.run(TestHarnessHostWorkflow, block)

  runBlocking {
    while (host.updates.receive().rendering === Running) {
    }
  }
}

internal typealias WorkflowTest = (TestWorkflowContext) -> Unit

internal sealed class HarnessState {
  object Running : HarnessState()
  object Success : HarnessState()
}

private object TestHarnessHostWorkflow
  : Workflow<WorkflowTest, HarnessState, Nothing, HarnessState> {

  override fun initialState(input: WorkflowTest): HarnessState = Running

  override fun compose(
    input: WorkflowTest,
    state: HarnessState,
    context: WorkflowContext<HarnessState, Nothing>
  ): HarnessState {
    if (state === Running) {
      context.compose(TEST_HARNESS_WORKFLOW, input) { noop() }
    }
    return state
  }

  override fun snapshotState(state: HarnessState): Snapshot = Snapshot.EMPTY
  override fun restoreState(snapshot: Snapshot): HarnessState = throw AssertionError()
}

private val TEST_HARNESS_WORKFLOW =
  StatelessWorkflow<WorkflowTest, Nothing, Unit> { input, context ->
    val testContext = TestWorkflowContext(context)
    input.invoke(testContext)
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
