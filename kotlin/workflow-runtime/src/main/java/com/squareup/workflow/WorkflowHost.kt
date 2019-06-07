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
package com.squareup.workflow

import com.squareup.workflow.WorkflowHost.Factory
import com.squareup.workflow.WorkflowHost.RenderingAndSnapshot
import com.squareup.workflow.internal.WorkflowNode
import com.squareup.workflow.internal.id
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

private val DEFAULT_WORKFLOW_COROUTINE_NAME = CoroutineName("WorkflowHost")

/**
 * Provides streams of renderings, snapshots, and outputs from a tree of [Workflow]s.
 *
 * Create these by injecting a [Factory] and calling [run][Factory.run].
 *
 * [cancel] **must** be called to shut down the workflow when you no longer care about it. If it
 * is not, the workflow will continue running forever.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
interface WorkflowHost<out OutputT : Any, out RenderingT> {

  /**
   * Emitted from [renderingsAndSnapshots] after every render pass.
   */
  data class RenderingAndSnapshot<out RenderingT>(
    val rendering: RenderingT,
    val snapshot: Snapshot
  )

  /**
   * Emits [renderings][RenderingT] and [snapshots][Snapshot] from the root [Workflow].
   *
   * The last-emitted [RenderingT] will be emitted immediately upon collection.
   */
  val renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>

  /**
   * Emits [outputs][OutputT] from the root [Workflow].
   */
  val outputs: Flow<OutputT>

  /**
   * Tears down the entire workflow runtime session, cancelling all workers and closing
   * [renderingsAndSnapshots] and [outputs].
   */
  fun cancel()

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
     * @param inputs Function that returns a channel that delivers input values for the root
     * workflow. The first value emitted is passed to [StatefulWorkflow.initialState] to determine
     * the root workflow's initial state, and subsequent emissions are passed as input updates to
     * the root workflow.
     * The channel returned by this function will be cancelled by the host when it's finished.
     * If [InputT] is `Unit`, you can just omit this argument.
     * @param snapshot If not null, used to restore the workflow.
     * @param context The [CoroutineContext] used to run the workflow tree. Added to the [Factory]'s
     * context.
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun <InputT, OutputT : Any, RenderingT> run(
      workflow: Workflow<InputT, OutputT, RenderingT>,
      inputs: () -> ReceiveChannel<InputT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<OutputT, RenderingT> =
      // Put the coroutine name first so the passed-in contexts can override it.
      RealWorkflowHost(DEFAULT_WORKFLOW_COROUTINE_NAME + baseContext + context) { ras, outputs ->
        runWorkflowTree(
            workflow = workflow.asStatefulWorkflow(),
            inputs = inputs,
            initialSnapshot = snapshot,
            onRendering = ras::send,
            onOutput = outputs::send
        )
      }

    fun <OutputT : Any, RenderingT> run(
      workflow: Workflow<Unit, OutputT, RenderingT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<OutputT, RenderingT> = run(workflow, channelOf(Unit), snapshot, context)

    /**
     * Creates a [WorkflowHost] that runs [workflow] starting from [initialState].
     *
     * **Don't call this directly.**
     *
     * Instead, your module should have a test dependency on `pure-v2-testing` and you should call
     * the testing extension method defined there on your workflow itself.
     */
    @TestOnly
    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun <InputT, StateT, OutputT : Any, RenderingT> runTestFromState(
      workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
      inputs: () -> ReceiveChannel<InputT>,
      initialState: StateT
    ): WorkflowHost<OutputT, RenderingT> =
      RealWorkflowHost(DEFAULT_WORKFLOW_COROUTINE_NAME + baseContext) { ras, outputs ->
        runWorkflowTree(
            workflow = workflow.asStatefulWorkflow(),
            inputs = inputs,
            initialSnapshot = null,
            initialState = initialState,
            onRendering = ras::send,
            onOutput = outputs::send
        )
      }

    private fun <T> channelOf(value: T): () -> ReceiveChannel<T> {
      return {
        Channel<T>(capacity = 1)
            .apply { offer(value) }
      }
    }
  }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
private class RealWorkflowHost<O : Any, R>(
  context: CoroutineContext,
  run: suspend (
    ras: SendChannel<RenderingAndSnapshot<R>>,
    outputs: SendChannel<O>
  ) -> Unit
) : WorkflowHost<O, R> {

  private val _renderings =
    BroadcastChannel<RenderingAndSnapshot<R>>(capacity = CONFLATED)

  // Ideally would have capacity of 0, but that's not allowed.
  private val _outputs = BroadcastChannel<O>(capacity = 1)

  private val job = GlobalScope.launch(context) {
    run(_renderings, _outputs)
  }

  @UseExperimental(FlowPreview::class)
  override val renderingsAndSnapshots: Flow<RenderingAndSnapshot<R>>
    get() = _renderings.asFlow()

  @UseExperimental(FlowPreview::class)
  override val outputs: Flow<O>
    get() = _outputs.asFlow()

  override fun cancel() {
    job.cancel()
  }
}

/**
 * Loops forever, or until the coroutine is cancelled, processing the workflow tree and emitting
 * updates by calling [onRendering] and [onOutput].
 *
 * This function is the lowest-level entry point into the runtime. Don't call this directly, instead
 * use [WorkflowHost.Factory] to create a [WorkflowHost].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
suspend fun <InputT, StateT, OutputT : Any, RenderingT> runWorkflowTree(
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  inputs: () -> ReceiveChannel<InputT>,
  initialSnapshot: Snapshot?,
  initialState: StateT? = null,
  onRendering: suspend (RenderingAndSnapshot<RenderingT>) -> Unit,
  onOutput: suspend (OutputT) -> Unit
): Nothing {
  val inputsChannel = inputs()
  inputsChannel.consume {
    var output: OutputT? = null
    var input: InputT = inputsChannel.receive()
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
        coroutineContext.ensureActive()

        val rendering = rootNode.render(workflow, input)
        val snapshot = rootNode.snapshot(workflow)

        onRendering(RenderingAndSnapshot(rendering, snapshot))
        if (output != null) onOutput(output)

        // Tick _might_ return an output, but if it returns null, it means the state or a child
        // probably changed, so we should re-render/snapshot and emit again.
        output = select {
          // Stop trying to read from the inputs channel after it's closed.
          if (!inputsClosed) {
            @Suppress("EXPERIMENTAL_API_USAGE")
            inputsChannel.onReceiveOrNull { newInput ->
              if (newInput == null) {
                inputsClosed = true
              } else {
                input = newInput
              }
              // No output. Returning from the select will go to the top of the loop to do another
              // render pass.
              return@onReceiveOrNull null
            }
          }

          // Tick the workflow tree.
          rootNode.tick(this) { it }
        }
      }
      // Compiler gets confused, and thinks both that this throw is unreachable, and without the
      // throw that the infinite while loop will exit normally and thus need a return statement.
      @Suppress("UNREACHABLE_CODE")
      throw AssertionError()
    } finally {
      // There's a potential race condition if the producer coroutine is cancelled before it has a
      // chance to enter the try block, since we can't use CoroutineStart.ATOMIC. However, until we
      // actually see this cause problems, I'm not too worried about it.
      // See https://github.com/Kotlin/kotlinx.coroutines/issues/845
      rootNode.cancel()
    }
  }
}
