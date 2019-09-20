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
package com.squareup.workflow.debugging

/**
 * A pair of [WorkflowHierarchyDebugSnapshot] and optional [WorkflowUpdateDebugInfo].
 *
 * The [WorkflowUpdateDebugInfo] will be null on the first render pass and non-null on every
 * subsequent pass, since the first pass is the only one that is not triggered by some kind of
 * update.
 */
data class WorkflowDebugInfo(
  val hierarchySnapshot: WorkflowHierarchyDebugSnapshot,
  val updateInfo: WorkflowUpdateDebugInfo?
)
