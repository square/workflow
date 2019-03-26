package com.squareup.workflow

import com.squareup.workflow.ExpectedOutput.Companion.expectNoOutput
import com.squareup.workflow.HarnessRendering.Expectations
import com.squareup.workflow.HarnessRendering.Success
import com.squareup.workflow.HarnessState.Initializing
import com.squareup.workflow.HarnessState.ValidatingOutput
import com.squareup.workflow.WorkflowAction.Companion.emitOutput
import com.squareup.workflow.WorkflowAction.Companion.enterState
import kotlinx.coroutines.experimental.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class WorkflowTest2Test {

  @Test fun stuff() {
    val wut = WUT()
    testWorkflow { context ->
      val rendering = context.compose(wut, "input")
      assertEquals("rendering", rendering)

    }
  }
}

interface ExpectedOutput<in OutputT : Any> {

  fun validateOutput(output: OutputT)

  companion object {
    fun <O : Any> expectNoOutput(): ExpectedOutput<O> = object : ExpectedOutput<O> {
      override fun validateOutput(output: O) {
        fail("Expected no output, got: $output")
      }
    }
  }
}

internal data class ActualOutput<out OutputT : Any>(
  val value: OutputT,
  val expectation: ExpectedOutput<@UnsafeVariance OutputT>
)

class TestWorkflowContext internal constructor(
  private val workflowContext: WorkflowContext<Unit, ActualOutput<*>>
) {

  fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any> compose(
    child: Workflow<InputT, StateT, OutputT, RenderingT>,
    input: InputT,
    expectedOutput: ExpectedOutput<OutputT> = expectNoOutput()
  ): RenderingT {
    return workflowContext.compose(child, input) { output ->
      emitOutput(ActualOutput(output, expectedOutput))
    }
  }

  internal fun buildExpectations(): ComposeExpectations {
    TODO()
  }
}

internal class ComposeExpectations

private fun testWorkflow(
  block: (TestWorkflowContext) -> Unit
) {
  val harness = TestHarnessWorkflow(block)
  val hostFactory = WorkflowHost.Factory(Dispatchers.Unconfined)
  val host = hostFactory.run(harness)

  TODO()
}

internal sealed class HarnessState {
  data class Initializing(val block: (TestWorkflowContext) -> Unit) : HarnessState()
  data class ValidatingOutput(val output: ActualOutput<*>) : HarnessState()
}

internal sealed class HarnessRendering {
  data class Expectations(val expectations: ComposeExpectations) : HarnessRendering()
  object Success : HarnessRendering()
}

private class TestHarnessHostWorkflow(
  private val testBlock: (TestWorkflowContext) -> Unit
) : Workflow<Unit, HarnessState, Nothing, Unit> {

  override fun initialState(input: Unit): HarnessState = Initializing(testBlock)

  override fun compose(
    input: Unit,
    state: HarnessState,
    context: WorkflowContext<HarnessState, Nothing>
  ) {
    val rendering = context.compose(TestHarnessWorkflow, state) { output ->
      enterState(ValidatingOutput(output))
    }
    when (rendering) {
      is Expectations -> TODO()
      Success -> TODO()
    }
  }

  override fun snapshotState(state: HarnessState): Snapshot = Snapshot.EMPTY
  override fun restoreState(snapshot: Snapshot): HarnessState = throw AssertionError()
}

private object TestHarnessWorkflow
  : Workflow<HarnessState, Unit, ActualOutput<*>, HarnessRendering> {

  override fun initialState(input: HarnessState): Unit = Unit

  override fun compose(
    input: HarnessState,
    state: Unit,
    context: WorkflowContext<Unit, ActualOutput<*>>
  ): HarnessRendering = when (input) {
    is Initializing -> {
      val testContext = TestWorkflowContext(context)
      input.block(testContext)
      Expectations(testContext.buildExpectations())
    }
    is ValidatingOutput -> {
      val (value, expectation) = input.output
      expectation.validateOutput(value)
      Success
    }
  }

  override fun snapshotState(state: Unit): Snapshot = Snapshot.EMPTY
  override fun restoreState(snapshot: Snapshot): Unit = Unit
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
