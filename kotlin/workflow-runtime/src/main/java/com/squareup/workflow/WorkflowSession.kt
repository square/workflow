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

import com.squareup.workflow.debugging.WorkflowDebugInfo
import kotlinx.coroutines.flow.Flow

/**
 * A tuple of [Flow]s representing all the emissions from the workflow runtime.
 *
 * Passed to the function taken by [launchWorkflowIn].
 *
 * @param debugInfo A stream of [diagnostic information][WorkflowDebugInfo] about the runtime.
 */
class WorkflowSession<out OutputT : Any, out RenderingT>(
  val renderingsAndSnapshots: Flow<RenderingAndSnapshot<RenderingT>>,
  val outputs: Flow<OutputT>,
  val debugInfo: Flow<WorkflowDebugInfo>
)
