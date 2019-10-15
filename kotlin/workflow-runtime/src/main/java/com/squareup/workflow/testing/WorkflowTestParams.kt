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
package com.squareup.workflow.testing

import com.squareup.workflow.Snapshot
import com.squareup.workflow.testing.WorkflowTestParams.StartMode
import com.squareup.workflow.testing.WorkflowTestParams.StartMode.StartFresh
import com.squareup.workflow.testing.WorkflowTestParams.StartMode.StartFromSnapshot
import com.squareup.workflow.testing.WorkflowTestParams.StartMode.StartFromState
import org.jetbrains.annotations.TestOnly

/**
 * Defines configuration for workflow testing infrastructure such as `testRender`, `testFromStart`.
 * and `test`.
 *
 * @param startFrom How to start the workflow – either [as a new workflow][StartFresh],
 * [from a snapshot][StartMode.StartFromSnapshot], or
 * [from a specific state][StartMode.StartFromState]. Defaults to [StartFresh].
 * @param checkRenderIdempotence If true, every render method will be called multiple times, to help
 * suss out any side effects that a render method is trying to perform. This parameter defaults to
 * `true` since the workflow contract is that `render` will be called an arbitrary number of times
 * for any given state, so performing side effects in `render` will almost always result in bugs.
 * It is recommended to leave this on, but if you need to debug a test and don't want to have to
 * deal with the extra passes, you can temporarily set it to false.
 */
@TestOnly
data class WorkflowTestParams<out StateT>(
  val startFrom: StartMode<StateT> = StartFresh,
  val checkRenderIdempotence: Boolean = true
) {
  /**
   * Defines how to start the workflow for tests.
   *
   * See the documentation on individual cases for more information:
   *  - [StartFresh]
   *  - [StartFromSnapshot]
   *  - [StartFromState]
   */
  sealed class StartMode<out StateT> {
    /**
     * Starts the workflow from its initial state (as specified by
     * [initial state][com.squareup.workflow.StatefulWorkflow.initialState]), with a null snapshot.
     */
    object StartFresh : StartMode<Nothing>()

    /**
     * Starts the workflow from its initial state (as specified by
     * [initial state][com.squareup.workflow.StatefulWorkflow.initialState]), with a non-null
     * snapshot.
     */
    data class StartFromSnapshot(val snapshot: Snapshot) : StartMode<Nothing>()

    /**
     * Starts the workflow from an exact state. Only applies to
     * [StatefulWorkflow][com.squareup.workflow.StatelessWorkflow]s.
     */
    data class StartFromState<StateT>(val state: StateT) : StartMode<StateT>()
  }
}
