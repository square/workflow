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
import com.squareup.workflow.internal.runWorkflowLoop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val DEFAULT_WORKFLOW_COROUTINE_NAME = CoroutineName("WorkflowHost")

/**
 * Provides streams of [renderings and snapshots][RenderingAndSnapshot] and outputs from a tree
 * of [Workflow]s.
 *
 * Create these by injecting a [Factory] and calling [run][Factory.run].
 *
 * [start] must be called to start the workflow running, and then [cancel] must be called to stop
 * it.
 */
interface WorkflowHost<out OutputT : Any, out RenderingT> {

  /**
   * Stream of [renderings and snapshots][RenderingAndSnapshot] from the root workflow.
   * Renderings and snapshots are always taken together, so they are emitted together.
   *
   * Once the [WorkflowHost] is [started][start], this Flow will immediately emit the first
   * rendering to any collectors, and then emit new renderings/snapshots any time something happens
   * within the workflow tree that causes a new render pass.
   *
   * New collectors arriving after calling [start] will always immediately get the last rendering
   * and snapshot.
   *
   * If any workflow or worker throws an exception, it will be re-thrown to collectors of this Flow
   * (although it may be wrapped in one or more [CancellationException]s).
   */
  @UseExperimental(ExperimentalCoroutinesApi::class)
  val renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>

  /**
   * Stream of outputs from the root workflow.
   *
   * This Flow is hot – it does *not* replay any old outputs to new collectors.
   *
   * If any workflow or worker throws an exception, it will be re-thrown to collectors of this Flow
   * (although it may be wrapped in one or more [CancellationException]s).
   */
  @UseExperimental(ExperimentalCoroutinesApi::class)
  val outputs: Flow<OutputT>

  /**
   * Start the workflow. [renderingsAndSnapshots] and [outputs] won't emit anything until this
   * is called. This method is idempotent – after the first call, all subsequent calls will return
   * the same [Job].
   *
   * This method gives the owner of this [WorkflowHost] the opportunity to start collecting
   * [outputs] before starting a workflow that could potentially emit an output right away.
   *
   * @return A [Job] that represents the running workflow tree. Cancelling this job will cancel the
   * workflow. However, if the [Factory]'s [baseContext][Factory.baseContext] contains a [Job], this
   * job will be a child of that job, so as long as that job is managed, this job needn't be
   * cancelled explicitly.
   */
  fun start(): Job

  /**
   * Inject one of these to start root [Workflow]s.
   *
   * @param baseContext The [CoroutineContext] for the coroutine that the workflow runtime is
   * invoked on. This context may be overridden by passing a context to any of the [run] methods.
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
      inputs: Flow<InputT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<OutputT, RenderingT> = RealWorkflowHost(
        // Put the coroutine name first so the passed-in contexts can override it.
        context = DEFAULT_WORKFLOW_COROUTINE_NAME + baseContext + context
    ) { onRendering, onOutput ->
      runWorkflowLoop(
          workflow = workflow.asStatefulWorkflow(),
          inputs = inputs,
          initialSnapshot = snapshot,
          onRendering = onRendering,
          onOutput = onOutput
      )
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun <OutputT : Any, RenderingT> run(
      workflow: Workflow<Unit, OutputT, RenderingT>,
      snapshot: Snapshot? = null,
      context: CoroutineContext = EmptyCoroutineContext
    ): WorkflowHost<OutputT, RenderingT> = run(workflow, flowOf(Unit), snapshot, context)

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
      inputs: Flow<InputT>,
      initialState: StateT
    ): WorkflowHost<OutputT, RenderingT> = RealWorkflowHost(
        context = DEFAULT_WORKFLOW_COROUTINE_NAME + baseContext
    ) { onRendering, onOutput ->
      runWorkflowLoop(
          workflow = workflow.asStatefulWorkflow(),
          inputs = inputs,
          initialSnapshot = null,
          initialState = initialState,
          onRendering = onRendering,
          onOutput = onOutput
      )
    }
  }
}
