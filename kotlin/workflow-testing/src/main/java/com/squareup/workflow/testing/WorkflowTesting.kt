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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow.testing

import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowHost
import com.squareup.workflow.WorkflowHost.Factory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <T, InputT : Any, OutputT : Any, RenderingT : Any>
    Workflow<InputT, OutputT, RenderingT>.testFromStart(
      input: InputT,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext,
      block: (WorkflowTester<OutputT, RenderingT>) -> T
    ): T = test(block, context) { it.run(this, input, snapshot) }
// @formatter:on

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
fun <T, OutputT : Any, RenderingT : Any> Workflow<Unit, OutputT, RenderingT>.testFromStart(
  snapshot: Snapshot? = null,
  context: CoroutineContext = EmptyCoroutineContext,
  block: (WorkflowTester<OutputT, RenderingT>) -> T
): T = testFromStart(Unit, snapshot, context, block)

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * If the workflow is [stateful][StatefulWorkflow], [initialState][StatefulWorkflow.initialState]
 * is not called. Instead, the workflow is started from the given [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <T, InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any>
    StatefulWorkflow<InputT, StateT, OutputT, RenderingT>.testFromState(
      input: InputT,
      initialState: StateT,
      context: CoroutineContext = EmptyCoroutineContext,
      block: (WorkflowTester<OutputT, RenderingT>) -> T
    ): T = test(block, context) { it.runTestFromState(this, input, initialState) }
// @formatter:on

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * If the workflow is [stateful][StatefulWorkflow], [initialState][StatefulWorkflow.initialState]
 * is not called. Instead, the workflow is started from the given [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <StateT : Any, OutputT : Any, RenderingT : Any>
    StatefulWorkflow<Unit, StateT, OutputT, RenderingT>.testFromState(
      initialState: StateT,
      context: CoroutineContext = EmptyCoroutineContext,
      block: (WorkflowTester<OutputT, RenderingT>) -> Unit
    ) = testFromState(Unit, initialState, context, block)
// @formatter:on

@UseExperimental(InternalCoroutinesApi::class)
private fun <T, O : Any, R : Any> test(
  testBlock: (WorkflowTester<O, R>) -> T,
  baseContext: CoroutineContext,
  starter: (Factory) -> WorkflowHost<O, R>
): T {
  val context = Dispatchers.Unconfined + baseContext + Job(parent = baseContext[Job])
  val host = WorkflowHost.Factory(context)
      .let(starter)
      .let { WorkflowTester(it, context) }

  var error: Throwable? = null
  try {
    return testBlock(host)
  } catch (e: Throwable) {
    error = e
    throw e
  } finally {
    if (error != null) {
      // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
      @Suppress("DEPRECATION")
      val cancelled = context.cancel(error)
      if (!cancelled) {
        val cancellationCause = context[Job]!!.getCancellationException()
            .cause
        if (cancellationCause != error && cancellationCause != null) {
          error.addSuppressed(cancellationCause)
        }
      }
    } else {
      // Cancel the Job to ensure everything gets cleaned up.
      context.cancel()
    }
  }
}
