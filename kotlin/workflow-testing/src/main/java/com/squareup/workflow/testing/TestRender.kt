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
@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package com.squareup.workflow.testing

import com.squareup.workflow.RenderContext
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.StatelessWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow

/**
 * Calls [StatelessWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * Child [Workflow]s should be stateless and not try to do anything with their [RenderContext] in
 * their render method. The [RenderContext] they get is not a real one, and will throw an exception
 * if it is used. In other words, the only thing child workflows can do is return a rendering. The
 * [MockChildWorkflow] abstract class makes it easy to write fake workflows that meet these
 * requirements, however you may use the normal ways of creating workflows too.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
@Deprecated("Use renderTester().")
fun <O : Any, R> StatelessWorkflow<Unit, O, R>.testRender(
  block: TestRenderResult<Unit, O, R>.() -> Unit
) = testRender(Unit, block)

/**
 * Calls [StatelessWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * Child [Workflow]s should be stateless and not try to do anything with their [RenderContext] in
 * their render method. The [RenderContext] they get is not a real one, and will throw an exception
 * if it is used. In other words, the only thing child workflows can do is return a rendering. The
 * [MockChildWorkflow] abstract class makes it easy to write fake workflows that meet these
 * requirements, however you may use the normal ways of creating workflows too.
 *
 * Child [Worker]s should be instances of [MockWorker].
 */
@Deprecated("Use renderTester().")
fun <I, O : Any, R> StatelessWorkflow<I, O, R>.testRender(
  input: I,
  block: TestRenderResult<Unit, O, R>.() -> Unit
) =
  @Suppress("UNCHECKED_CAST")
  (asStatefulWorkflow() as StatefulWorkflow<I, Unit, O, R>)
      .testRender(input, Unit, block)

/**
 * Calls [StatefulWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * Child [Workflow]s should be stateless and not try to do anything with their [RenderContext] in
 * their render method. The [RenderContext] they get is not a real one, and will throw an exception
 * if it is used. In other words, the only thing child workflows can do is return a rendering. The
 * [MockChildWorkflow] abstract class makes it easy to write fake workflows that meet these
 * requirements, however you may use the normal ways of creating workflows too.
 *
 * Child [Worker]s should be instances of [MockWorker].
 *
 * Use [testRenderInitialState] to automatically calculate the initial state from the input.
 */
@Deprecated("Use renderTester().")
fun <S, O : Any, R> StatefulWorkflow<Unit, S, O, R>.testRender(
  state: S,
  block: TestRenderResult<S, O, R>.() -> Unit
) = testRender(Unit, state, block)

/**
 * Calls [StatefulWorkflow.render] and returns a [TestRenderResult] that can be used to get at
 * the actual [rendering][TestRenderResult.rendering], assert on which children and workers were
 * run, and execute the output handlers for rendered children and workers to assert on their
 * state transitions and emitted output.
 *
 * ## Mocking Child Workflows and Workers
 *
 * Child [Workflow]s should be stateless and not try to do anything with their [RenderContext] in
 * their render method. The [RenderContext] they get is not a real one, and will throw an exception
 * if it is used. In other words, the only thing child workflows can do is return a rendering. The
 * [MockChildWorkflow] abstract class makes it easy to write fake workflows that meet these
 * requirements, however you may use the normal ways of creating workflows too.
 *
 * Child [Worker]s should be instances of [MockWorker].
 *
 * Use [testRenderInitialState] to automatically calculate the initial state from the input.
 */
@Deprecated("Use renderTester().")
fun <I, S, O : Any, R> StatefulWorkflow<I, S, O, R>.testRender(
  props: I,
  state: S,
  block: TestRenderResult<S, O, R>.() -> Unit
) {
  val testRenderContext = TestOnlyRenderContext<S, O>()
  val rendering = render(props, state, testRenderContext)

  // Run the render pass one more time to catch render methods that try to do side effects.
  render(props, state, TestOnlyRenderContext())

  val result = TestRenderResult(rendering, state, testRenderContext.buildBehavior())
  result.block()
}

/**
 * Calls [StatefulWorkflow.initialState] to get the initial state for the [input], then calls
 * [testRender] with that state and passes through the [TestRenderResult] to [block] along with
 * the initial state.
 */
@Deprecated("Use renderTester().")
fun <I, S, O : Any, R> StatefulWorkflow<I, S, O, R>.testRenderInitialState(
  input: I,
  block: TestRenderResult<S, O, R>.(initialState: S) -> Unit
) {
  val state = initialState(input, snapshot = null)
  testRender(input, state) { block(state) }
}

/**
 * Calls [StatefulWorkflow.initialState] to get the initial state, then calls
 * [testRender] with that state and passes through the [TestRenderResult] to [block] along with
 * the initial state.
 */
@Deprecated("Use renderTester().")
fun <S, O : Any, R> StatefulWorkflow<Unit, S, O, R>.testRenderInitialState(
  block: TestRenderResult<S, O, R>.(initialState: S) -> Unit
) {
  testRenderInitialState(Unit, block)
}
