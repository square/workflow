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

import com.squareup.workflow.internal.RealWorkflowLoop
import com.squareup.workflow.internal.WorkflowLoop
import com.squareup.workflow.internal.unwrapCancellationCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CancellationException

/**
 * Don't use this typealias for the public API, better to just use the function directly so it's
 * more obvious how to use it.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
internal typealias Configurator <O, R, T> = CoroutineScope.(
  renderingsAndSnapshots: Flow<RenderingAndSnapshot<R>>,
  outputs: Flow<O>
) -> T

/**
 * Launches the [workflow] in a new coroutine in [scope]. The workflow tree is seeded with
 * [initialSnapshot] and the first value emitted by [inputs].  Subsequent values emitted from
 * [inputs] will be used to re-render the workflow.
 *
 * This is the primary low-level entry point into the workflow runtime. If you are writing an app,
 * you should probably be using a higher-level entry point that will also let you define UI bindings
 * for your renderings.
 *
 * ## Initialization
 *
 * Before starting the actual workflow runtime, this function will invoke [beforeStart] and pass
 * it the [Flow]s of renderings, snapshots, and outputs, as well as a [CoroutineScope] that the
 * runtime will be hosted in. The workflow runtime will not be started until after this function
 * returns. This is to allow the output flow to start being collected before any outputs can
 * actually be emitted. Collectors that start _after_ [beforeStart] returns may not receive outputs
 * that are emitted very quickly. The value returned by [beforeStart] will be returned from this
 * function after the runtime is launched, handy for instantiating platform-specific runner objects.
 * The [CoroutineScope] passed to [beforeStart] can be used to do more than just cancel the runtime
 * – it can also be used to start coroutines that will run until the workflow is cancelled,
 * typically to collect the rendering and output [Flow]s.
 *
 * ## Scoping
 *
 * The workflow runtime makes use of
 * [structured concurrency](https://medium.com/@elizarov/structured-concurrency-722d765aa952).
 * The runtime is started in a specific [CoroutineScope], which defines the context for the entire
 * workflow tree – most importantly, the [Job] that governs the runtime's lifetime and exception
 * reporting path, and the [CoroutineDispatcher][kotlinx.coroutines.CoroutineDispatcher] that
 * decides on what threads to run workflow code.
 *
 * This function creates a child [Job] in [scope] and uses it as the parent for the workflow
 * runtime, and as the job passed to the [beforeStart] function. This means that if [beforeStart]
 * calls [CoroutineScope.cancel][Job.cancel], it will cancel the workflow runtime, but will not cancel the
 * [scope] passed into this function.
 *
 * @param scope The [CoroutineScope] in which to launch the workflow runtime. Any exceptions thrown
 * in any workflows will be reported to this scope, and cancelling this scope will cancel the
 * workflow runtime. The scope passed to [beforeStart] will be a child of this scope.
 * @param workflow The root workflow to start.
 * @param inputs Stream of input values for the root workflow. The first value emitted is passed to
 * the root workflow's [StatefulWorkflow.initialState], and subsequent emissions are passed as
 * input updates to the root workflow. If this flow completes before emitting anything, the runtime
 * will fail (report an exception up through [scope]). If this flow completes _after_ emitting at
 * least one value, the runtime will _not_ fail or stop, it will continue running with the
 * last-emitted input.
 * @param initialSnapshot If not null, used to restore the workflow.
 * @param beforeStart Called exactly once with the flows for renderings/snapshots and outputs.
 * It also gets a sub-scope of [scope] with a newly created child [Job] which defines the lifetime
 * of the launched workflow tree. Cancelling that job ends the new workflow session.
 * Note that if [scope] is already cancelled when this function is called, the receiver scope will
 * also be cancelled, and the flows will complete immediately.
 *
 * @return The value returned by [beforeStart].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <InputT, OutputT : Any, RenderingT, RunnerT> launchWorkflowIn(
  scope: CoroutineScope,
  workflow: Workflow<InputT, OutputT, RenderingT>,
  inputs: Flow<InputT>,
  initialSnapshot: Snapshot? = null,
  beforeStart: CoroutineScope.(
    renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>,
    outputs: Flow<OutputT>
  ) -> RunnerT
): RunnerT = launchWorkflowImpl(
    scope,
    RealWorkflowLoop,
    workflow.asStatefulWorkflow(),
    inputs,
    initialSnapshot = initialSnapshot,
    initialState = null,
    beforeStart = beforeStart
)

/**
 * Launches the [workflow] in a new coroutine in [scope]. The workflow tree is seeded with
 * [initialSnapshot] and the first value emitted by [inputs].  Subsequent values emitted from
 * [inputs] will be used to re-render the workflow.
 *
 * Like [launchWorkflowIn], but will automatically cancel the workflow as soon as the first
 * [output][OutputT] is emitted. [beforeStart] gets the same [Flow] of [RenderingAndSnapshot], but
 * instead of a flow of outputs it gets a [Deferred] of the first output.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <InputT, OutputT : Any, RenderingT, ResultT> launchSingleOutputWorkflowIn(
  scope: CoroutineScope,
  workflow: Workflow<InputT, OutputT, RenderingT>,
  inputs: Flow<InputT>,
  initialSnapshot: Snapshot? = null,
  beforeStart: CoroutineScope.(
    renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>,
    output: Deferred<OutputT>
  ) -> ResultT
): ResultT = launchWorkflowIn(scope, workflow, inputs, initialSnapshot) { renderings, outputs ->
  val workflowScope = this
  val output = async { outputs.first() }.apply {
    invokeOnCompletion {
      workflowScope.cancel(CancellationException("Workflow finished normally."))
    }
  }
  beforeStart(renderings, output)
}

/**
 * Launches the [workflow] in a new coroutine in [scope]. The workflow tree is seeded with
 * [initialState] and the first value emitted by [inputs].  Subsequent values emitted from
 * [inputs] will be used to re-render the workflow.
 *
 * See [launchWorkflowIn] for documentation about most of the parameters and behavior.
 *
 * @param initialState The [StateT] in which to start the root workflow.
 */
@TestOnly
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <InputT, StateT, OutputT : Any, RenderingT, RunnerT> launchWorkflowForTestFromStateIn(
  scope: CoroutineScope,
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  inputs: Flow<InputT>,
  initialState: StateT,
  beforeStart: CoroutineScope.(
    renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>,
    outputs: Flow<OutputT>
  ) -> RunnerT
): RunnerT = launchWorkflowImpl(
    scope,
    RealWorkflowLoop,
    workflow,
    inputs,
    initialState = initialState,
    initialSnapshot = null,
    beforeStart = beforeStart
)

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal fun <InputT, StateT, OutputT : Any, RenderingT, RunnerT> launchWorkflowImpl(
  scope: CoroutineScope,
  workflowLoop: WorkflowLoop,
  workflow: StatefulWorkflow<InputT, StateT, OutputT, RenderingT>,
  inputs: Flow<InputT>,
  initialSnapshot: Snapshot?,
  initialState: StateT?,
  beforeStart: Configurator<OutputT, RenderingT, RunnerT>
): RunnerT {
  val renderingsAndSnapshots = ConflatedBroadcastChannel<RenderingAndSnapshot<RenderingT>>()
  val outputs = BroadcastChannel<OutputT>(capacity = 1)
  val workflowJob = Job(parent = scope.coroutineContext[Job])
  val workflowScope = scope + workflowJob

  // Ensure we close the channels when we're done, so that they propagate errors.
  workflowJob.invokeOnCompletion { cause ->
    // We need to unwrap the cancellation exception so that we *complete* the channels instead
    // of cancelling them if our coroutine was merely cancelled.
    val realCause = cause?.unwrapCancellationCause()
    renderingsAndSnapshots.close(realCause)
    outputs.close(realCause)
  }

  // Give the caller a chance to start collecting outputs.
  val result = beforeStart(workflowScope, renderingsAndSnapshots.asFlow(), outputs.asFlow())

  workflowScope.launch {
    // Run the workflow processing loop forever, or until it fails or is cancelled.
    workflowLoop.runWorkflowLoop(
        workflow,
        inputs,
        initialSnapshot = initialSnapshot,
        initialState = initialState,
        onRendering = renderingsAndSnapshots::send,
        onOutput = outputs::send
    )
  }

  return result
}
