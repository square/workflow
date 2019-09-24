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
 * ## Detekt Configuration
 *
 * If you're using Detekt, you may need to suppress some warnings when implementing this interface.
 *
 * - Add `@Suppress("TooManyFunctions")` to your class.
 * - Add `@Suppress("LongParameterList")` to `onWorkflowStarted` if you override it.
 */
@Suppress("TooManyFunctions")
interface WorkflowDiagnosticListener {

  // region Render Measurement

  /**
   * Called just before the workflow tree is going to be rendered.
   *
   * Corresponds to [onAfterRenderPass].
   */
  @VeryExperimentalWorkflow
  fun onBeforeRenderPass(props: Any?) = Unit

  /**
   * TODO kdoc
   *
   * @param workflowId If null, the props for the root workflow changed. States will always be null
   * in that case.
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
   * Called just after an individual workflow's render method is going to be called.
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
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onBeforeSnapshotPass() = Unit

  /**
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onAfterSnapshotPass() = Unit

  // endregion

  // region Lifetime tracking

  /**
   * Called when the runtime has started executing.
   *
   * No other methods will be called until after this returns.
   */
  @VeryExperimentalWorkflow
  fun onRuntimeStarted(workflowScope: CoroutineScope) = Unit

  /**
   * Called after the runtime has been cancelled or failed, after all workflow-related coroutines
   * have completed.
   *
   * No other methods will be called after this.
   */
  @VeryExperimentalWorkflow
  fun onRuntimeStopped() = Unit

  /**
   * Called when a particular workflow node is started at a particular point in the workflow
   * tree.
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
   */
  @VeryExperimentalWorkflow
  fun onWorkflowStopped(workflowId: Long) = Unit

  /**
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onWorkerStarted(
    workerId: Long,
    parentWorkflowId: Long,
    key: String,
    description: String
  ) = Unit

  /**
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onWorkerStopped(
    workerId: Long,
    parentWorkflowId: Long
  ) = Unit

  // endregion

  // region Updates

  /**
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onWorkerOutput(
    workerId: Long,
    parentWorkflowId: Long,
    output: Any
  ) = Unit

  /**
   * TODO kdoc
   */
  @VeryExperimentalWorkflow
  fun onSinkReceived(
    workflowId: Long,
    action: WorkflowAction<*, *>
  ) = Unit

  /**
   * TODO kdoc
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
