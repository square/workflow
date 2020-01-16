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
import com.squareup.workflow.internal.id
import com.squareup.workflow.internal.unwrapCancellationCause
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Don't use this typealias for the public API, better to just use the function directly so it's
 * more obvious how to use it.
 */
internal typealias Configurator <O, R, T> = CoroutineScope.(
  session: WorkflowSession<O, R>
) -> T

/**
 * Launches the [workflow] in a new coroutine in [scope]. The workflow tree is seeded with
 * [initialSnapshot] and the first value emitted by [props].  Subsequent values emitted from
 * [props] will be used to re-render the workflow.
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
 * @param props Stream of input values for the root workflow. The first value emitted is passed to
 * the root workflow's [StatefulWorkflow.initialState], and subsequent emissions are passed as
 * input updates to the root workflow. If this flow completes before emitting anything, the runtime
 * will fail (report an exception up through [scope]). If this flow completes _after_ emitting at
 * least one value, the runtime will _not_ fail or stop, it will continue running with the
 * last-emitted input.
 * @param initialSnapshot If not null or empty, used to restore the workflow.
 * @param beforeStart Called exactly once with the flows for renderings/snapshots and outputs.
 * It also gets a sub-scope of [scope] with a newly created child [Job] which defines the lifetime
 * of the launched workflow tree. Cancelling that job ends the new workflow session.
 * Note that if [scope] is already cancelled when this function is called, the receiver scope will
 * also be cancelled, and the flows will complete immediately.
 *
 * @return The value returned by [beforeStart].
 */
fun <PropsT, OutputT : Any, RenderingT, RunnerT> launchWorkflowIn(
  scope: CoroutineScope,
  workflow: Workflow<PropsT, OutputT, RenderingT>,
  props: Flow<PropsT>,
  initialSnapshot: Snapshot? = null,
  beforeStart: CoroutineScope.(session: WorkflowSession<OutputT, RenderingT>) -> RunnerT
): RunnerT = launchWorkflowImpl(
    scope,
    RealWorkflowLoop(),
    workflow.asStatefulWorkflow(),
    props,
    initialSnapshot = initialSnapshot,
    initialState = null,
    beforeStart = beforeStart
)

@UseExperimental(
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
    VeryExperimentalWorkflow::class
)
@Suppress("LongParameterList")
internal fun <PropsT, StateT, OutputT : Any, RenderingT, RunnerT> launchWorkflowImpl(
  scope: CoroutineScope,
  workflowLoop: WorkflowLoop,
  workflow: StatefulWorkflow<PropsT, StateT, OutputT, RenderingT>,
  props: Flow<PropsT>,
  initialSnapshot: Snapshot?,
  initialState: StateT?,
  beforeStart: Configurator<OutputT, RenderingT, RunnerT>
): RunnerT {
  val renderingsAndSnapshots = ConflatedBroadcastChannel<RenderingAndSnapshot<RenderingT>>()
  val outputs = BroadcastChannel<OutputT>(capacity = 1)
  val workflowScope = scope + Job(parent = scope.coroutineContext[Job])

  // Give the caller a chance to start collecting outputs.
  val session = WorkflowSession(renderingsAndSnapshots.asFlow(), outputs.asFlow())
  val result = beforeStart(workflowScope, session)
  val diagnosticListener = session.diagnosticListener

  val workflowJob = workflowScope.launch {
    diagnosticListener?.onRuntimeStarted(this, workflow.id().typeDebugString)
    try {
      // Run the workflow processing loop forever, or until it fails or is cancelled.
      workflowLoop.runWorkflowLoop(
          workflow,
          props,
          initialSnapshot = initialSnapshot,
          initialState = initialState,
          onRendering = renderingsAndSnapshots::send,
          onOutput = outputs::send,
          diagnosticListener = diagnosticListener
      )
    } finally {
      // Only emit the runtime stopped debug event after all child coroutines have completed.
      // coroutineScope does an implicit join on all its children.
      diagnosticListener?.onRuntimeStopped()
    }
  }

  // Ensure we close the channels when we're done, so that they propagate errors.
  workflowJob.invokeOnCompletion { cause ->
    // We need to unwrap the cancellation exception so that we *complete* the channels instead
    // of cancelling them if our coroutine was merely cancelled.
    val realCause = cause?.unwrapCancellationCause()
    renderingsAndSnapshots.close(realCause)
    outputs.close(realCause)

    // If the cancellation came from inside the workflow loop, the outer runtime scope needs to be
    // explicitly cancelled. See https://github.com/square/workflow/issues/464.
    workflowScope.coroutineContext[Job]!!.let {
      if (!it.isCancelled) {
        it.cancel(
            realCause as? CancellationException
                ?: CancellationException("Workflow cancelled", realCause)
        )
      }
    }
  }

  return result
}
