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

import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.applyTo
import com.squareup.workflow.testing.RealRenderTester.Expectation.ExpectedWorker
import com.squareup.workflow.testing.RealRenderTester.Expectation.ExpectedWorkflow
import kotlin.reflect.KClass

internal class RealRenderTester<PropsT, StateT, OutputT : Any, RenderingT>(
  private val workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
  private val props: PropsT,
  private val state: StateT,
  private val expectations: MutableList<Expectation<*>> = mutableListOf(),
  private val consumedExpectations: MutableList<Expectation<*>> = mutableListOf(),
  private var childWillEmitOutput: Boolean = false,
  private var processedAction: WorkflowAction<StateT, OutputT>? = null
) : RenderTester<PropsT, StateT, OutputT, RenderingT>,
    RenderContext<StateT, OutputT>,
    RenderTestResult<StateT, OutputT>,
    Sink<WorkflowAction<StateT, OutputT>> {

  internal sealed class Expectation<out OutputT> {
    abstract val output: EmittedOutput<OutputT>?

    data class ExpectedWorkflow<OutputT : Any, RenderingT>(
      val workflowType: KClass<out Workflow<*, OutputT, RenderingT>>,
      val key: String,
      val assertProps: (props: Any?) -> Unit,
      val rendering: RenderingT,
      override val output: EmittedOutput<OutputT>?
    ) : Expectation<OutputT>()

    data class ExpectedWorker<out OutputT>(
      val matchesWhen: (otherWorker: Worker<*>) -> Boolean,
      val key: String,
      override val output: EmittedOutput<OutputT>?
    ) : Expectation<OutputT>()
  }

  override val actionSink: Sink<WorkflowAction<StateT, OutputT>> get() = this

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> expectWorkflow(
    workflowType: KClass<out Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>>,
    rendering: ChildRenderingT,
    key: String,
    assertProps: (props: ChildPropsT) -> Unit,
    output: EmittedOutput<ChildOutputT>?
  ): RenderTester<PropsT, StateT, OutputT, RenderingT> {
    @Suppress("UNCHECKED_CAST")
    val assertAnyProps = { props: Any? -> assertProps(props as ChildPropsT) }
    val expectedWorkflow = ExpectedWorkflow(workflowType, key, assertAnyProps, rendering, output)
    if (output != null) {
      checkNoOutputs(expectedWorkflow)
      childWillEmitOutput = true
    }
    expectations += expectedWorkflow
    return this
  }

  override fun expectWorker(
    matchesWhen: (otherWorker: Worker<*>) -> Boolean,
    key: String,
    output: EmittedOutput<Any?>?
  ): RenderTester<PropsT, StateT, OutputT, RenderingT> {
    val expectedWorker = ExpectedWorker(matchesWhen, key, output)
    if (output != null) {
      checkNoOutputs(expectedWorker)
      childWillEmitOutput = true
    }
    expectations += expectedWorker
    return this
  }

  override fun render(block: (RenderingT) -> Unit): RenderTestResult<StateT, OutputT> {
    // Clone the expectations to run a "dry" render pass.
    val noopContext = deepCloneForRender()
    workflow.render(props, state, noopContext)

    workflow.render(props, state, this)
        .also(block)

    // Ensure all expected children ran.
    if (expectations.isNotEmpty()) {
      throw AssertionError(
          "Expected ${expectations.size} more workflows or workers to be ran:\n" +
              expectations.joinToString(separator = "\n") { "  $it" }
      )
    }

    return this
  }

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT {
    fun describeWorkflow() = "child workflow ${child::class.java.name}" +
        key.takeUnless { it.isEmpty() }
            ?.let { " with key \"$it\"" }
            .orEmpty()

    val expected = consumeExpectation<ExpectedWorkflow<*, *>>(
        predicate = { it.workflowType.isInstance(child) && it.key == key },
        description = ::describeWorkflow,
        onNoExpectationsMatched = {
          throw AssertionError("Tried to render unexpected ${describeWorkflow()}.")
        }
    )

    expected.assertProps(props)

    if (expected.output != null) {
      check(processedAction == null)
      @Suppress("UNCHECKED_CAST")
      processedAction = handler(expected.output.output as ChildOutputT)
    }

    @Suppress("UNCHECKED_CAST")
    return expected.rendering as ChildRenderingT
  }

  override fun <T> runningWorker(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<StateT, OutputT>
  ) {
    val expected = consumeExpectation<ExpectedWorker<*>?>(
        predicate = { it!!.matchesWhen(worker) && it.key == key },
        description = {
          "worker $worker" +
              key.takeUnless { it.isEmpty() }
                  ?.let { " with key \"$it\"" }
                  .orEmpty()
        },
        onNoExpectationsMatched = { null }
    ) ?: return

    if (expected.output != null) {
      check(processedAction == null)
      @Suppress("UNCHECKED_CAST")
      processedAction = handler(expected.output.output as T)
    }
  }

  override fun send(value: WorkflowAction<StateT, OutputT>) {
    checkNoOutputs()
    check(processedAction == null) {
      "Tried to send action to sink after another action was already processed:\n" +
          "  processed action=$processedAction\n" +
          "  attempted action=$value"
    }
    processedAction = value
  }

  override fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): (EventT) -> Unit = { event -> send(handler(event)) }

  override fun verifyAction(block: (WorkflowAction<StateT, OutputT>) -> Unit) {
    val action = processedAction ?: noAction()
    block(action)
  }

  override fun verifyActionResult(block: (StateT, OutputT?) -> Unit) {
    verifyAction { action ->
      val (newState, output) = action.applyTo(state)
      block(newState, output)
    }
  }

  private inline fun <reified T : Expectation<*>?> consumeExpectation(
    predicate: (T) -> Boolean,
    description: () -> String,
    onNoExpectationsMatched: () -> T
  ): T {
    val matchedExpectations = expectations.filterIsInstance<T>()
        .filter(predicate)
    return when (matchedExpectations.size) {
      0 -> onNoExpectationsMatched()
      1 -> {
        matchedExpectations[0].also {
          // it can't be null, even if T is nullable, because expectations doesn't contain nulls.
          it as Expectation<*>
          // Move the workflow to the consumed list.
          expectations -= it
          consumedExpectations += it
        }
      }
      else -> throw AssertionError(
          "Multiple expectations matched ${description()}:\n" +
              matchedExpectations.joinToString(separator = "\n") { "  $it" }
      )
    }
  }

  private fun deepCloneForRender(): RenderContext<StateT, OutputT> = RealRenderTester(
      workflow, props, state,
      // Copy the list of expectations since it's mutable.
      expectations = ArrayList(expectations),
      // Don't care about consumed expectations.
      childWillEmitOutput = childWillEmitOutput,
      processedAction = processedAction
  )

  private fun checkNoOutputs(newExpectation: Expectation<*>? = null) {
    check(!childWillEmitOutput) {
      val expectationsWithOutputs = (expectations + listOfNotNull(newExpectation))
          .filter { it.output != null }
      "Expected only one child to emit an output:\n" +
          expectationsWithOutputs.joinToString(separator = "\n") { "  $it" }
    }
  }
}
