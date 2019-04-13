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

package com.squareup.workflow

import com.squareup.workflow.WorkflowHost.Factory
import com.squareup.workflow.WorkflowHost.Update
import com.squareup.workflow.internal.WorkflowNode
import com.squareup.workflow.internal.id
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Provides a stream of [updates][Update] from a tree of [Workflow]s.
 *
 * Create these by injecting a [Factory] and calling [run][Factory.run].
 */
interface WorkflowHost<in InputT : Any, out OutputT : Any, out RenderingT : Any> {

  /**
   * Output from a [WorkflowHost]. Emitted from [WorkflowHost.updates] after every compose pass.
   */
  data class Update<out OutputT : Any, out RenderingT : Any>(
    val rendering: RenderingT,
    val snapshot: Snapshot,
    val output: OutputT? = null
  )

  /**
   * Stream of [updates][Update] from the root workflow.
   *
   * This is *not* a broadcast channel, so it should only be read by a single consumer.
   */
  val updates: ReceiveChannel<Update<OutputT, RenderingT>>

  /**
   * Inject one of these to start root [Workflow]s.
   */
  class Factory(private val baseContext: CoroutineContext) {

    /**
     * Creates a [WorkflowHost] to run [workflow].
     *
     * The workflow's initial state is determined by passing the first value emitted by [inputs] to
     * [StatefulWorkflow.initialState]. Subsequent values emitted from [inputs] will be used to
     * re-render the workflow.
     *
     * @param workflow The workflow to start.
     * @param inputs Passed to [StatefulWorkflow.initialState] to determine the root workflow's
     * initial state, and to pass input updates to the root workflow.
     * If [InputT] is `Unit`, you can just omit this argument.
     * @param snapshot If not null, used to restore the workflow.
     * @param context The [CoroutineContext] used to run the workflow tree. Added to the [Factory]'s
     * context.
     */
    fun <InputT : Any, OutputT : Any, RenderingT : Any> run(
      workflow: Workflow<InputT, OutputT, RenderingT>,
      inputs: ReceiveChannel<InputT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<InputT, OutputT, RenderingT> =
      object : WorkflowHost<InputT, OutputT, RenderingT> {
        private val scope = CoroutineScope(context)

        override val updates: ReceiveChannel<Update<OutputT, RenderingT>> =
          scope.produce(capacity = 0) {
            runWorkflowTree(
                workflow = workflow.asStatefulWorkflow(),
                inputs = inputs,
                initialSnapshot = snapshot,
                onUpdate = ::send
            )
          }
      }

    fun <OutputT : Any, RenderingT : Any> run(
      workflow: Workflow<Unit, OutputT, RenderingT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<Unit, OutputT, RenderingT> = run(workflow, channelOf(Unit), snapshot, context)

    /**
     * Creates a [WorkflowHost] that runs [workflow] starting from [initialState].
     *
     * **Don't call this directly.**
     *
     * Instead, your module should have a test dependency on `pure-v2-testing` and you should call
     * the testing extension method defined there on your workflow itself.
     */
    @TestOnly
    fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any> runTestFromState(
      workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
      input: InputT,
      initialState: StateT
    ): WorkflowHost<InputT, OutputT, RenderingT> =
      object : WorkflowHost<InputT, OutputT, RenderingT> {
        override val updates: ReceiveChannel<Update<OutputT, RenderingT>> =
          GlobalScope.produce(capacity = 0, context = baseContext) {
            runWorkflowTree(
                workflow = workflow.asStatefulWorkflow(),
                inputs = channelOf(input),
                initialSnapshot = null,
                initialState = initialState,
                onUpdate = ::send
            )
          }
      }

    private fun <T> channelOf(value: T) = Channel<T>(capacity = 1)
        .apply { offer(value) }
  }
}

/**
 * Loops forever, or until the coroutine is cancelled, processing the workflow tree and emitting
 * updates by calling [onUpdate].
 *
 * This function is the lowest-level entry point into the runtime. Don't call this directly, instead
 * use [WorkflowHost.Factory] to create a [WorkflowHost], or one of the stream operators for your
 * favorite Rx library to map a stream of [InputT]s into [Update]s.
 */
@UseExperimental(InternalCoroutinesApi::class)
suspend fun <InputT : Any, StateT : Any, OutputT : Any, RenderingT : Any> runWorkflowTree(
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  inputs: ReceiveChannel<InputT>,
  initialSnapshot: Snapshot?,
  initialState: StateT? = null,
  onUpdate: suspend (Update<OutputT, RenderingT>) -> Unit
): Nothing {
  var output: OutputT? = null
  var input: InputT = inputs.receive()
  var inputsClosed = false
  val rootNode = WorkflowNode(
      id = workflow.id(),
      workflow = workflow,
      initialInput = input,
      snapshot = initialSnapshot,
      baseContext = coroutineContext,
      initialState = initialState
  )

  try {
    while (true) {
      // Manually implement `ensureActive()` until we're on coroutines 1.2.x.
      if (!isActive) throw coroutineContext[Job]!!.getCancellationException()

      val rendering = rootNode.compose(workflow, input)
      val snapshot = rootNode.snapshot(workflow)

      onUpdate(Update(rendering, snapshot, output))

      // Tick _might_ return an output, but if it returns null, it means the state or a child
      // probably changed, so we should re-compose/snapshot and emit again.
      output = select {
        // Stop trying to read from the inputs channel after it's closed.
        if (!inputsClosed) {
          @Suppress("EXPERIMENTAL_API_USAGE")
          inputs.onReceiveOrNull { newInput ->
            if (newInput == null) {
              inputsClosed = true
            } else {
              input = newInput
            }
            // No output. Returning from the select will go to the top of the loop to do another
            // compose pass.
            return@onReceiveOrNull null
          }
        }

        // Tick the workflow tree.
        rootNode.tick(this) { it }
      }
    }
  } catch (e: Throwable) {
    // For some reason the exception gets masked if we don't explicitly pass it to cancel the
    // producer coroutine ourselves here.
    // TODO https://github.com/square/workflow/issues/188 Stop using parameterized cancel.
    @Suppress("DEPRECATION")
    coroutineContext.cancel(e)
    throw e
  } finally {
    // There's a potential race condition if the producer coroutine is cancelled before it has a
    // chance to enter the try block, since we can't use CoroutineStart.ATOMIC. However, until we
    // actually see this cause problems, I'm not too worried about it.
    rootNode.cancel()
  }
}
