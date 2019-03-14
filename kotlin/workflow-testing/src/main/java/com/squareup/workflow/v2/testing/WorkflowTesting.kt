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
package com.squareup.workflow.v2.testing

import com.squareup.workflow.legacy.Snapshot
import com.squareup.workflow.v2.Workflow
import com.squareup.workflow.v2.WorkflowHost
import com.squareup.workflow.v2.WorkflowHost.Factory
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancel
import org.jetbrains.annotations.TestOnly

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <InputT : Any, OutputT : Any, RenderingT : Any>
    Workflow<InputT, *, OutputT, RenderingT>.testFromStart(
      input: InputT,
      snapshot: Snapshot? = null,
      block: (WorkflowTester<OutputT, RenderingT>) -> Unit
    ) = test(block) { it.run(this, input, snapshot) }
// @formatter:on

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
fun <OutputT : Any, RenderingT : Any> Workflow<Unit, *, OutputT, RenderingT>.testFromStart(
  snapshot: Snapshot? = null,
  block: (WorkflowTester<OutputT, RenderingT>) -> Unit
) = testFromStart(Unit, snapshot, block)

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * [Workflow.initialState] is not called. Instead, the workflow is started from the given
 * [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any>
    Workflow<InputT, StateT, OutputT, RenderingT>.testFromState(
      input: InputT,
      initialState: StateT,
      block: (WorkflowTester<OutputT, RenderingT>) -> Unit
    ) = test(block) { it.runTestFromState(this, input, initialState) }
// @formatter:on

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * [Workflow.initialState] is not called. Instead, the workflow is started from the given
 * [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <StateT : Any, OutputT : Any, RenderingT : Any>
    Workflow<Unit, StateT, OutputT, RenderingT>.testFromState(
      initialState: StateT,
      block: (WorkflowTester<OutputT, RenderingT>) -> Unit
    ) = testFromState(Unit, initialState, block)
// @formatter:on

private fun <O : Any, R : Any> test(
  testBlock: (WorkflowTester<O, R>) -> Unit,
  starter: (Factory) -> WorkflowHost<O, R>
) {
  val context = Job() + Dispatchers.Unconfined
  val host = WorkflowHost.Factory(context)
      .let(starter)
      .let { WorkflowTester(it, context) }

  var error: Throwable? = null
  try {
    testBlock(host)
  } catch (e: Throwable) {
    error = e
    throw e
  } finally {
    if (error != null) {
      val cancelled = context.cancel(error)
      if (!cancelled) {
        val cancellationCause = context[Job]!!.getCancellationException()
            .cause
        if (cancellationCause != error) {
          error.addSuppressed(cancellationCause)
        }
      }
    } else {
      // Cancel the Job to ensure everything gets cleaned up.
      context.cancel()
    }
  }
}
