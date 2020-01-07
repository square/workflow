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
package com.squareup.workflow.diagnostic

import com.squareup.workflow.VeryExperimentalWorkflow
import com.squareup.workflow.WorkflowAction
import kotlinx.coroutines.CoroutineScope

/**
 * Receives diagnostic events from the workflow runtime when significant things happen, e.g. the
 * runtime starts or stops, workflows and workers are started or stopped, and information about
 * render passes and workflow actions.
 *
 * To register a listener with the runtime, set
 * [WorkflowSession.diagnosticListener][com.squareup.workflow.WorkflowSession.diagnosticListener]
 * in the `beforeStart` function passed to
 * [launchWorkflowIn][com.squareup.workflow.launchWorkflowIn].
 *
 * Multiple listeners can be composed using the [andThen] function.
 *
 * No guarantees are made about what threads methods are invoked on. Implementations should take
 * care to be thread-safe.
 *
 * ## Sequence of Events
 *
 * Events received by this interface occur in a well-defined order.
 *
 * 1. [onRuntimeStarted]
 * 1. [onBeforeRenderPass]
 *     1. [onBeforeWorkflowRendered]
 *         - Also called for each child workflow, recursively.
 *         - Child start are emitted before the render method returns.
 *           1. [onWorkflowStarted]
 *           1. [onWorkerStarted]
 *     1. [onAfterWorkflowRendered]
 *     - Child stop events are emitted after the render method returns.
 *       - [onWorkflowStopped]
 *       - [onWorkerStopped]
 * 1. [onAfterRenderPass]
 * 1. [onBeforeSnapshotPass]
 * 1. [onAfterSnapshotPass]
 * 1. Either:
 *     - [onSinkReceived]
 *     - [onWorkerOutput]
 * 1. [onWorkflowAction]
 * 1. Back to [onBeforeRenderPass].
 * 1. [onRuntimeStopped]
 *
 * ## Detekt Configuration
 *
 * If you're using Detekt, you may need to suppress some warnings when implementing this interface.
 *
 * - Add `@Suppress("TooManyFunctions")` to your class.
 * - Add `@Suppress("LongParameterList")` to [onWorkflowStarted] if you override it.
 */
@Suppress("TooManyFunctions")
interface WorkflowDiagnosticListener {

  // region Render Events

  /**
   * Called just before the workflow tree is going to be rendered.
   *
   * Corresponds to [onAfterRenderPass].
   */
  @VeryExperimentalWorkflow
  fun onBeforeRenderPass(props: Any?) = Unit

  /**
   * Called after [onPropsChanged][com.squareup.workflow.StatefulWorkflow.onPropsChanged] returns.
   *
   * @param workflowId The ID of the workflow for this event. If null, the props for the root
   * workflow changed. [oldState] and [newState] will always be null in that case.
   */
  @VeryExperimentalWorkflow
  fun onPropsChanged(
    workflowId: Long?,
    oldProps: Any?,
    newProps: Any?,
    oldState: Any?,
    newState: Any?
  ) = Unit

  /**
   * Called just before an individual workflow's render method is going to be called.
   *
   * Corresponds to [onAfterWorkflowRendered].
   */
  @VeryExperimentalWorkflow
  fun onBeforeWorkflowRendered(
    workflowId: Long,
    props: Any?,
    state: Any?
  ) = Unit

  /**
   * Called just after an individual workflow's render method was called.
   *
   * Corresponds to [onBeforeWorkflowRendered].
   */
  @VeryExperimentalWorkflow
  fun onAfterWorkflowRendered(
    workflowId: Long,
    rendering: Any?
  ) = Unit

  /**
   * Called just after the workflow tree finishes rendering.
   *
   * Corresponds to [onBeforeRenderPass].
   */
  @VeryExperimentalWorkflow
  fun onAfterRenderPass(rendering: Any?) = Unit

  /**
   * Called just before all the workflows'
   * [snapshotState][com.squareup.workflow.StatefulWorkflow.snapshotState] methods are called.
   *
   * Corresponds to [onAfterSnapshotPass].
   */
  @VeryExperimentalWorkflow
  fun onBeforeSnapshotPass() = Unit

  /**
   * Called just after all the workflows'
   * [snapshotState][com.squareup.workflow.StatefulWorkflow.snapshotState] methods are called.
   *
   * Corresponds to [onBeforeSnapshotPass].
   */
  @VeryExperimentalWorkflow
  fun onAfterSnapshotPass() = Unit

  // endregion

  // region Lifetime Events

  /**
   * Called when the runtime has started executing.
   *
   * No other methods will be called on this interface before this returns.
   *
   * Corresponds to [onRuntimeStopped].
   */
  @VeryExperimentalWorkflow
  fun onRuntimeStarted(
    workflowScope: CoroutineScope,
    rootWorkflowType: String
  ) = Unit

  /**
   * Called after the runtime has been cancelled or failed, after all workflow-related coroutines
   * have completed.
   *
   * Corresponds to [onRuntimeStarted].
   */
  @VeryExperimentalWorkflow
  fun onRuntimeStopped() = Unit

  /**
   * Called when a particular workflow node is started at a particular point in the workflow
   * tree.
   *
   * Corresponds to [onWorkerStopped].
   *
   * @param workflowId The ID of the workflow that just started.
   * @param parentId The ID of the workflow whose call to
   * [renderChild][com.squareup.workflow.RenderContext.renderChild] caused this workflow to start.
   * @param workflowType The fully-qualified name of this workflow's class.
   * @param key The key passed to `renderChild` by [parentId] when rendering this workflow.
   * @param initialProps The props passed to `renderChild` by [parentId].
   * @param initialState The state returned from
   * [initialState][com.squareup.workflow.StatefulWorkflow.initialState].
   * @param restoredFromSnapshot True iff the snapshot parameter to `initialState` was non-null.
   */
  @VeryExperimentalWorkflow
  @Suppress("LongParameterList")
  fun onWorkflowStarted(
    workflowId: Long,
    parentId: Long?,
    workflowType: String,
    key: String,
    initialProps: Any?,
    initialState: Any?,
    restoredFromSnapshot: Boolean
  ) = Unit

  /**
   * Called when a particular workflow node is stopped at a particular point in the workflow tree.
   *
   * Corresponds to [onWorkflowStarted].
   *
   * @param workflowId The ID of the workflow that stopped.
   */
  @VeryExperimentalWorkflow
  fun onWorkflowStopped(workflowId: Long) = Unit

  /**
   * Called when a workflow starts running a new worker.
   *
   * Corresponds to [onWorkerStopped].
   *
   * @param workerId An globally-unique ID that uniquely identifies the worker.
   * @param parentWorkflowId The ID of the workflow whose call to
   * [runningWorker][com.squareup.workflow.RenderContext.runningWorker] caused this worker to start.
   * @param key The key passed to `runningWorker` by [parentWorkflowId].
   * @param description A string description of the worker. Contains the worker's `toString`.
   */
  @VeryExperimentalWorkflow
  fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) = Unit

  /**
   * Called when a worker finishes running. The workflow may still technically be "running" the
   * worker, but it will never emit any more outputs.
   *
   * Corresponds to [onWorkerStarted].
   *
   * @param workerId The ID of the worker that stopped.
   * @param parentWorkflowId The ID of the workflow that was running this worker.
   */
  @VeryExperimentalWorkflow
  fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) = Unit

  // endregion

  // region Update Events

  /**
   * Called when a worker emits an output.
   *
   * [onWorkflowAction] will always be called after this.
   *
   * @param workerId The ID of the worker that emitted [output].
   * @param parentWorkflowId The ID of the workflow that is running this worker and that will
   * receive the output.
   * @param output The value that the worker output.
   */
  @VeryExperimentalWorkflow
  fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) = Unit

  /**
   * Called when a [Sink][com.squareup.workflow.Sink] has received an event.
   *
   * [onWorkflowAction] will always be called after this.
   *
   * @param workflowId The ID of the workflow that created the sink and will execute its
   * corresponding [WorkflowAction].
   * @param action The [WorkflowAction] that will be executed by [workflowId].
   */
  @VeryExperimentalWorkflow
  fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) = Unit

  /**
   * Called when a workflow has executed a [WorkflowAction] in response to a worker output or sink
   * event.
   *
   * Either [onWorkerOutput] or [onSinkReceived] will have been called before this.
   *
   * @param workflowId The ID of the workflow that created [action].
   * @param action The [WorkflowAction] that was executed.
   * @param oldState The state of the workflow before executing [action].
   * @param newState The state of the workflow after executing [action]. If the action doesn't set
   * the state, this will be the same value as [oldState].
   * @param output The output value returned from [action].
   */
  @VeryExperimentalWorkflow
  fun onWorkflowAction(
    workflowId: Long,
    action: WorkflowAction<*, *>,
    oldState: Any?,
    newState: Any?,
    output: Any?
  ) = Unit

  // endregion
}
