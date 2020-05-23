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
@file:Suppress("EXPERIMENTAL_API_USAGE", "DeprecatedCallableAddReplaceWith")

package com.squareup.workflow.testing

import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Workflow
import com.squareup.workflow.internal.util.UncaughtExceptionGuard
import com.squareup.workflow.testing.WorkflowTestParams.StartMode.StartFromState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
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
 *  - [sendProps]
 *    - Send a new [PropsT] to the root workflow.
 */
class WorkflowTester<PropsT, OutputT : Any, RenderingT> @TestOnly internal constructor(
  private val scope: CoroutineScope,
  private val props: SendChannel<PropsT>,
  private val renderingsAndSnapshotsFlow: Flow<RenderingAndSnapshot<RenderingT>>,
  private val outputsFlow: Flow<OutputT>
) {

  private val renderings = Channel<RenderingT>(capacity = UNLIMITED)
  private val snapshots = Channel<Snapshot>(capacity = UNLIMITED)
  private val outputs = Channel<OutputT>(capacity = UNLIMITED)

  internal fun collectFromWorkflow() {
    // Subscribe before starting to ensure we get all the emissions.
    // We use NonCancellable so that if context is already cancelled, the operator chains below
    // are still allowed to handle the exceptions from WorkflowHost streams explicitly, since they
    // need to close the test channels.
    val realScope = scope + NonCancellable
    renderingsAndSnapshotsFlow
        .onEach { (rendering, snapshot) ->
          renderings.send(rendering)
          snapshots.send(snapshot)
        }
        .onCompletion { e ->
          renderings.close(e)
          snapshots.close(e)
        }
        .launchIn(realScope)

    outputsFlow
        .onEach { outputs.send(it) }
        .onCompletion { e -> outputs.close(e) }
        .launchIn(realScope)
  }

  /**
   * Runs the test from [block], and then cancels the workflow runtime after it's done.
   */
  internal fun <T> runTest(block: WorkflowTester<PropsT, OutputT, RenderingT>.() -> T): T =
    try {
      block(this)
    } finally {
      scope.cancel(CancellationException("Test finished"))
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
  fun sendProps(input: PropsT) {
    runBlocking {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        props.send(input)
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
   * The returned snapshot will be the snapshot only of the root workflow. It will be null if
   * `snapshotState` returned an empty [Snapshot].
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
    const val DEFAULT_TIMEOUT_MS: Long = 500
  }
}

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
fun <T, PropsT, OutputT : Any, RenderingT> Workflow<PropsT, OutputT, RenderingT>.testFromStart(
  props: PropsT,
  testParams: WorkflowTestParams<Nothing> = WorkflowTestParams(),
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<PropsT, OutputT, RenderingT>.() -> T
): T = asStatefulWorkflow().test(props, testParams, context, block)

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
fun <T, OutputT : Any, RenderingT> Workflow<Unit, OutputT, RenderingT>.testFromStart(
  testParams: WorkflowTestParams<Nothing> = WorkflowTestParams(),
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<Unit, OutputT, RenderingT>.() -> T
): T = testFromStart(Unit, testParams, context, block)

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * If the workflow is [stateful][StatefulWorkflow], [initialState][StatefulWorkflow.initialState]
 * is not called. Instead, the workflow is started from the given [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
/* ktlint-disable parameter-list-wrapping */
@TestOnly
fun <T, PropsT, StateT, OutputT : Any, RenderingT>
    StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>.testFromState(
  props: PropsT,
  initialState: StateT,
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<PropsT, OutputT, RenderingT>.() -> T
): T = test(props, WorkflowTestParams(StartFromState(initialState)), context, block)
/* ktlint-enable parameter-list-wrapping */

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 * If the workflow is [stateful][StatefulWorkflow], [initialState][StatefulWorkflow.initialState]
 * is not called. Instead, the workflow is started from the given [initialState].
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
/* ktlint-disable parameter-list-wrapping */
@TestOnly
fun <StateT, OutputT : Any, RenderingT>
    StatefulWorkflow<Unit, StateT, OutputT, RenderingT>.testFromState(
  initialState: StateT,
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<Unit, OutputT, RenderingT>.() -> Unit
) = testFromState(Unit, initialState, context, block)
/* ktlint-enable parameter-list-wrapping */

/**
 * Creates a [WorkflowTester] to run this workflow for unit testing.
 *
 * All workflow-related coroutines are cancelled when the block exits.
 */
@TestOnly
/* ktlint-disable parameter-list-wrapping */
fun <T, PropsT, StateT, OutputT : Any, RenderingT>
    StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>.test(
  props: PropsT,
  testParams: WorkflowTestParams<StateT> = WorkflowTestParams(),
  context: CoroutineContext = EmptyCoroutineContext,
  block: WorkflowTester<PropsT, OutputT, RenderingT>.() -> T
): T {
  /* ktlint-enable parameter-list-wrapping */
  val propsChannel = ConflatedBroadcastChannel(props)

  // Any exceptions that are thrown from a launch will be reported to the coroutine's uncaught
  // exception handler, which will, by default, report them to the thread. We don't want to do that,
  // we want to throw them from the test directly so they'll fail the test and/or let the test code
  // assert on them.
  val exceptionGuard = UncaughtExceptionGuard()
  val uncaughtExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    exceptionGuard.reportUncaught(throwable)
  }

  // TODO(https://github.com/square/workflow/issues/1192) Migrate to renderWorkflowIn.
  val tester = launchWorkflowForTestFromStateIn(
      scope = CoroutineScope(Unconfined + context + uncaughtExceptionHandler),
      workflow = this@test,
      props = propsChannel.asFlow(),
      testParams = testParams
  ) { session ->
    WorkflowTester(this, propsChannel, session.renderingsAndSnapshots, session.outputs)
        .apply { collectFromWorkflow() }
  }

  return exceptionGuard.runRethrowingUncaught {
    tester.runTest(block)
  }
}
