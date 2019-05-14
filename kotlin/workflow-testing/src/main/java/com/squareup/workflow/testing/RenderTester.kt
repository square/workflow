/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.testing

import com.squareup.workflow.EventHandler
import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Worker.OutputOrFinished.Finished
import com.squareup.workflow.Worker.OutputOrFinished.Output
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.Behavior
import com.squareup.workflow.internal.Behavior.WorkerCase
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import com.squareup.workflow.internal.RealRenderContext
import com.squareup.workflow.internal.RealRenderContext.Renderer
import com.squareup.workflow.internal.WorkflowId

/**
 * Calls [StatelessWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * All child [Workflow]s must be instances of [MockChildWorkflow], as they are not given a working
 * [RenderContext]. This function only allows you to test a single workflow's render method.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
fun <O : Any, R> StatelessWorkflow<Unit, O, R>.testRender(): TestRenderResult<Unit, O, R> =
  testRender(Unit)

/**
 * Calls [StatelessWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * All child [Workflow]s must be instances of [MockChildWorkflow], as they are not given a working
 * [RenderContext]. This function only allows you to test a single workflow's render method.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
fun <I, O : Any, R> StatelessWorkflow<I, O, R>.testRender(input: I): TestRenderResult<Unit, O, R> =
  @Suppress("UNCHECKED_CAST")
  (asStatefulWorkflow() as StatefulWorkflow<I, Unit, O, R>)
      .testRender(input, Unit)

/**
 * Calls [StatefulWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * All child [Workflow]s must be instances of [MockChildWorkflow], as they are not given a working
 * [RenderContext]. This function only allows you to test a single workflow's render method.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
fun <S, O : Any, R> StatefulWorkflow<Unit, S, O, R>.testRender(
  state: S
): TestRenderResult<S, O, R> = testRender(Unit, state)

/**
 * Calls [StatefulWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * All child [Workflow]s must be instances of [MockChildWorkflow], as they are not given a working
 * [RenderContext]. This function only allows you to test a single workflow's render method.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
fun <I, S, O : Any, R> StatefulWorkflow<I, S, O, R>.testRender(
  input: I,
  state: S
): TestRenderResult<S, O, R> {
  val testRenderContext = RealRenderContext(object : Renderer<S, O> {
    override fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> render(
      case: WorkflowOutputCase<ChildInputT, ChildOutputT, S, O>,
      child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
      id: WorkflowId<ChildInputT, ChildOutputT, ChildRenderingT>,
      input: ChildInputT
    ): ChildRenderingT {
      require(child is MockChildWorkflow) {
        "Expected child workflow to be a MockChildWorkflow: $id"
      }

      @Suppress("UNCHECKED_CAST")
      val childStatefulWorkflow =
        child.asStatefulWorkflow() as StatefulWorkflow<ChildInputT, Any?, ChildOutputT, ChildRenderingT>
      val childInitialState = childStatefulWorkflow.initialState(input, null)
      // Allow the workflow-under-test to *render* children, but those children must not try to
      // use the RenderContext themselves.
      return childStatefulWorkflow.render(input, childInitialState, NoopRenderContext)
    }
  })

  val rendering = render(input, state, testRenderContext)
  return TestRenderResult(rendering, state, testRenderContext.buildBehavior())
}

/**
 * Represents the result of running a single render pass on a workflow.
 *
 * @param rendering The actual [RenderingT] value returned from the workflow's `render` method.
 * @param state The [StateT] passed into the `render` method.
 * @param behavior The [Behavior] generated from the [RenderContext].
 */
class TestRenderResult<StateT, OutputT : Any, RenderingT> internal constructor(
  val rendering: RenderingT,
  private val state: StateT,
  private val behavior: Behavior<StateT, OutputT>
) {

  /**
   * Throws an [AssertionError] if the render pass did not render [workflow] with [key].
   */
  fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> assertWorkflowRendered(
    workflow: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    key: String = ""
  ) {
    findWorkflowCase(workflow, key)
  }

  /**
   * Asserts that [workflow] was rendered with the given [key] and then executes the output handler
   * with the given [output] (as an [Output]). Returns the new state and output returned by the
   * output handler.
   */
  fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> executeWorkflowActionFromOutput(
    workflow: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    output: ChildOutputT,
    key: String = ""
  ): Pair<StateT, OutputT?> {
    val case = findWorkflowCase(workflow, key)
    val action = case.acceptChildOutput(output)
    return action(state)
  }

  /**
   * Throws an [AssertionError] if the render pass did not run [worker] with [key].
   */
  fun <T : Any> assertWorkerRan(
    worker: Worker<T>,
    key: String = ""
  ) {
    findWorkerCase(worker, key)
  }

  /**
   * Asserts that [worker] was ran with the given [key] and then executes the output handler
   * with the given [output] (as an [Output]). Returns the new state and output returned by the
   * output handler.
   */
  fun <T : Any> executeWorkerActionFromOutput(
    worker: Worker<T>,
    output: T,
    key: String = ""
  ): Pair<StateT, OutputT?> = executeWorkerAction(worker, Output(output), key)

  /**
   * Asserts that [worker] was ran with the given [key] and then executes the output handler
   * with [Finished]. Returns the new state and output returned by the output handler.
   */
  fun <T : Any> executeWorkerActionFromFinish(
    worker: Worker<T>,
    key: String = ""
  ): Pair<StateT, OutputT?> = executeWorkerAction(worker, Finished, key)

  /**
   * Throws an [AssertionError] if any [Workflow]s were rendered.
   */
  fun assertNoWorkflowsRendered() {
    behavior.childCases.let {
      if (it.isNotEmpty()) {
        throw AssertionError("Expected no workflows to be rendered, but ${it.size} were.")
      }
    }
  }

  /**
   * Throws an [AssertionError] if any [Worker]s were run.
   */
  fun assertNoWorkersRan() {
    behavior.workerCases.let {
      if (it.isNotEmpty()) {
        throw AssertionError("Expected no workflows to be rendered, but ${it.size} were.")
      }
    }
  }

  private fun <T : Any> executeWorkerAction(
    worker: Worker<T>,
    outputOrFinished: OutputOrFinished<T>,
    key: String = ""
  ): Pair<StateT, OutputT?> {
    val case = findWorkerCase(worker, key)
    val action = case.acceptUpdate(outputOrFinished)
    return action(state)
  }

  private fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> findWorkflowCase(
    workflow: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    key: String
  ): WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT> {
    val id = WorkflowId(workflow, key)
    @Suppress("UNCHECKED_CAST")
    return behavior.childCases.singleOrNull { it.id == id }
        as WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT>?
        ?: throw AssertionError("Expected workflow to be rendered: $workflow (key=\"$key\")")
  }

  private fun <T : Any> findWorkerCase(
    worker: Worker<T>,
    key: String
  ): WorkerCase<T, StateT, OutputT> {
    @Suppress("UNCHECKED_CAST")
    return behavior.workerCases.singleOrNull { it.worker.doesSameWorkAs(worker) && it.key == key }
        as WorkerCase<T, StateT, OutputT>?
        ?: throw AssertionError("Expected worker to be rendered: $worker (key=\"$key\")")
  }
}

private object NoopRenderContext : RenderContext<Any?, Any> {
  override fun <EventT : Any> onEvent(handler: (EventT) -> WorkflowAction<Any?, Any>): EventHandler<EventT> {
    throw UnsupportedOperationException()
  }

  override fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    input: ChildInputT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<Any?, Any>
  ): ChildRenderingT {
    throw UnsupportedOperationException()
  }

  override fun <T> onWorkerOutputOrFinished(
    worker: Worker<T>,
    key: String,
    handler: (OutputOrFinished<T>) -> WorkflowAction<Any?, Any>
  ) {
    throw UnsupportedOperationException()
  }
}
