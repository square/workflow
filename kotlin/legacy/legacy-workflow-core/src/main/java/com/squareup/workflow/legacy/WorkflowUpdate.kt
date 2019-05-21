/*
 * Copyright 2018 Square Inc.
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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.legacy

/**
 * Reports either the [new state][Running] or the [result][Finished] of
 * a [Workflow] being run via [WorkflowPool.awaitWorkflowUpdate].
 */
@Deprecated("Use com.squareup.workflow.Workflow")
sealed class WorkflowUpdate<out S : Any, out E : Any, out O : Any>

/**
 * Reports that a [Workflow] started by a previous call to [WorkflowPool.awaitWorkflowUpdate]
 * is still running after a state change. [handle] carries the new state.
 *
 * Receiver must use [handle] to either:
 *  - keep the [Workflow] running, by calling [WorkflowPool.awaitWorkflowUpdate] again, or
 *  - abandon the [Workflow] by calling [WorkflowPool.abandonWorker]
 */
@Deprecated("Use com.squareup.workflow.Workflow")
data class Running<S : Any, E : Any, O : Any>(
  val handle: WorkflowPool.Handle<S, E, O>
) : WorkflowUpdate<S, E, O>()

/**
 * Reports that a [Workflow] started by a previous call to [WorkflowPool.awaitWorkflowUpdate]
 * has completed its work, and provides its [result].
 */
@Deprecated("Use com.squareup.workflow.Workflow")
data class Finished<O : Any>(val result: O) : WorkflowUpdate<Nothing, Nothing, O>()
