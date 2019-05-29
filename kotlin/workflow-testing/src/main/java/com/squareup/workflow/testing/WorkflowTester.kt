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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Runs a [Workflow][com.squareup.workflow.Workflow] and provides access to its
 * [renderings][awaitNextRendering], [outputs][awaitNextOutput], and [snapshots][awaitNextSnapshot].
 *
 * For each of renderings, outputs, and snapshots, this class gives you a few ways to access
 * information about them:
 *  - [awaitNextRendering], [awaitNextOutput], [awaitNextSnapshot]
 *    - Block until something becomes available, and then return it.
 *  - [hasRendering], [hasOutput], [hasSnapshot]
 *    - Return `true` if the previous methods won't block.
 *  - [sendInput]
 *    - Send a new [InputT] to the root workflow.
 */
class WorkflowTester<InputT, OutputT : Any, RenderingT> @TestOnly internal constructor(
  private val inputs: SendChannel<InputT>,
  private val host: WorkflowHost<OutputT, RenderingT>,
  context: CoroutineContext
) {

  private val renderings = Channel<RenderingT>(capacity = UNLIMITED)
  private val snapshots = Channel<Snapshot>(capacity = UNLIMITED)
  private val outputs = Channel<OutputT>(capacity = UNLIMITED)

  init {
    val job = CoroutineScope(context)
        .launch {
          host.updates.consumeEach { (rendering, snapshot, output) ->
            renderings.send(rendering)
            snapshots.send(snapshot)
            output?.let { outputs.send(it) }
          }
        }
    job.invokeOnCompletion { cause ->
      renderings.close(cause)
      snapshots.close(cause)
      outputs.close(cause)
    }
  }

  /**
   * True if the workflow has emitted a new rendering that is ready to be consumed.
   */
  val hasRendering: Boolean get() = !renderings.isEmptyOrClosed

  /**
   * True if the workflow has emitted a new snapshot that is ready to be consumed.
   */
  val hasSnapshot: Boolean get() = !snapshots.isEmptyOrClosed

  /**
   * True if the workflow has emitted a new output that is ready to be consumed.
   */
  val hasOutput: Boolean get() = !outputs.isEmptyOrClosed

  private val ReceiveChannel<*>.isEmptyOrClosed get() = isEmpty || isClosedForReceive

  /**
   * Sends [input] to the workflow.
   */
  fun sendInput(input: InputT) {
    runBlocking {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        inputs.send(input)
      }
    }
  }

  /**
   * Blocks until the workflow emits a rendering, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for a rendering to be emitted. If null,
   * [WorkflowTester.DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple renderings, all but the
   * most recent one will be dropped.
   */
  fun awaitNextRendering(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true
  ): RenderingT = renderings.receiveBlocking(timeoutMs, skipIntermediate)

  /**
   * Blocks until the workflow emits a snapshot, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for a snapshot to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   * @param skipIntermediate If true, and the workflow has emitted multiple snapshots, all but the
   * most recent one will be dropped.
   */
  fun awaitNextSnapshot(
    timeoutMs: Long? = null,
    skipIntermediate: Boolean = true
  ): Snapshot = snapshots.receiveBlocking(timeoutMs, skipIntermediate)

  /**
   * Blocks until the workflow emits an output, then returns it.
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   */
  fun awaitNextOutput(timeoutMs: Long? = null): OutputT =
    outputs.receiveBlocking(timeoutMs, drain = false)

  /**
   * Blocks until the workflow fails by throwing an exception, then returns that exception.
   *
   * @param timeoutMs The maximum amount of time to wait for an output to be emitted. If null,
   * [DEFAULT_TIMEOUT_MS] will be used instead.
   */
  fun awaitFailure(timeoutMs: Long? = null): Throwable {
    var error: Throwable? = null
    runBlocking {
      withTimeout(timeoutMs ?: DEFAULT_TIMEOUT_MS) {
        try {
          while (true) renderings.receive()
        } catch (e: Throwable) {
          error = e
        }
      }
    }
    return error!!
  }

  /**
   * @param drain If true, this function will consume all the values currently in the channel, and
   * return the last one.
   */
  private fun <T> ReceiveChannel<T>.receiveBlocking(
    timeoutMs: Long?,
    drain: Boolean
  ): T = runBlocking {
    withTimeout(timeoutMs ?: DEFAULT_TIMEOUT_MS) {
      var item = receive()
      if (drain) {
        while (!isEmpty) {
          item = receive()
        }
      }
      return@withTimeout item
    }
  }

  companion object {
    const val DEFAULT_TIMEOUT_MS: Long = 5000
  }
}

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
// @formatter:off
@TestOnly
fun <T, InputT, OutputT : Any, RenderingT>
    Workflow<InputT, OutputT, RenderingT>.testFromStart(
      input: InputT,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext,
      block: WorkflowTester<InputT, OutputT, RenderingT>.() -> T
    ): T = test(block, context) { factory, inputs ->
      inputs.offer(input)
      factory.run(this, inputs, snapshot)
    }
// @formatter:on

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
fun <T, OutputT : Any, RenderingT> Workflow<Unit, OutputT, RenderingT>.testFromStart(
  snapshot: Snapshot? = null,
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<Unit, OutputT, RenderingT>.() -> T
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
fun <T, InputT, StateT, OutputT : Any, RenderingT>
    StatefulWorkflow<InputT, StateT, OutputT, RenderingT>.testFromState(
      input: InputT,
      initialState: StateT,
      context: CoroutineContext = EmptyCoroutineContext,
      block: WorkflowTester<InputT, OutputT, RenderingT>.() -> T
    ): T = test(block, context) { factory, inputs ->
      inputs.offer(input)
      factory.runTestFromState(this, inputs, initialState)
    }
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
fun <StateT, OutputT : Any, RenderingT>
    StatefulWorkflow<Unit, StateT, OutputT, RenderingT>.testFromState(
      initialState: StateT,
      context: CoroutineContext = EmptyCoroutineContext,
      block: WorkflowTester<Unit, OutputT, RenderingT>.() -> Unit
    ) = testFromState(Unit, initialState, context, block)
// @formatter:on

@UseExperimental(InternalCoroutinesApi::class)
private fun <T, I, O : Any, R> test(
  testBlock: (WorkflowTester<I, O, R>) -> T,
  baseContext: CoroutineContext,
  starter: (WorkflowHost.Factory, inputs: Channel<I>) -> WorkflowHost<O, R>
): T {
  val context = Dispatchers.Unconfined + baseContext + Job(parent = baseContext[Job])
  val inputs = Channel<I>(capacity = 1)
  @Suppress("ReplaceSingleLineLet")
  val host = WorkflowHost.Factory(context)
      .let { starter(it, inputs) }
      .let { WorkflowTester(inputs, it, context) }

  var error: Throwable? = null
  try {
    return testBlock(host)
  } catch (e: Throwable) {
    error = e
    throw e
  } finally {
    if (error != null) {
      context.cancel(
          if (error is CancellationException) error else CancellationException(null, error)
      )
      val cancellationCause = context[Job]!!.getCancellationException()
          .cause
      if (cancellationCause != error && cancellationCause != null) {
        error.addSuppressed(cancellationCause)
      }
    } else {
      // Cancel the Job to ensure everything gets cleaned up.
      context.cancel()
    }
  }
}
