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

import com.squareup.workflow.WorkflowPool.Id
import com.squareup.workflow.WorkflowPool.Launcher
import com.squareup.workflow.WorkflowPool.Type

/**
 * Events that can be reported by a [WorkflowPool].
 *
 * The sequence of possible events for a given workflow is as follows:
 *  TODO flow chart
 */
sealed class WorkflowPoolMonitorEvent {

  /**
   * Emitted when a workflow is [registered][WorkflowPool.register] on a [WorkflowPool].
   */
  data class Registered(
    val type: Type<*, *, *>,
    val launcher: Launcher<*, *, *>
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted as soon as [WorkflowPool.awaitWorkflowUpdate] or [WorkflowPool.awaitWorkerResult] are
   * called, before the update is actually dispatched.
   */
  data class UpdateRequested(
    val id: Id<*, *, *>,
    val state: Any
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted when the [WorkflowPool] launches a new instance of a workflow.
   */
  data class Launched(
    val id: Id<*, *, *>,
    val initialState: Any
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted every time a [Workflow] emits a new state.
   */
  data class StateChanged(
    val id: Id<*, *, *>,
    val newState: Any
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted every time an event is sent to the [Workflow].
   */
  data class ReceivedEvent(
    val id: Id<*, *, *>,
    val event: Any
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted when a [Workflow] completes.
   *
   * @see RemovedFromPool
   */
  data class Finished(
    val id: Id<*, *, *>,
    val result: Any
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted when the [WorkflowPool] is asked to abandon a workflow.
   */
  data class Abandoned(
    val id: Id<*, *, *>
  ) : WorkflowPoolMonitorEvent()

  /**
   * Emitted when the [WorkflowPool] removes a completed or abandoned [Workflow] from its pool.
   */
  data class RemovedFromPool(
    val id: Id<*, *, *>
  ) : WorkflowPoolMonitorEvent()
}
