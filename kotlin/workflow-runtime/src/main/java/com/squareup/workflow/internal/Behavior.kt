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
package com.squareup.workflow.internal

import com.squareup.workflow.Worker
import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import kotlinx.coroutines.Deferred

/**
 * An immutable description of the things a [Workflow] would like to do as the result of calling its
 * `render` method. A `Behavior` is built up by calling methods on a
 * [RenderContext][com.squareup.workflow.RenderContext] ([RealRenderContext] in particular).
 *
 * @see RealRenderContext
 */
internal data class Behavior<StateT : Any, out OutputT : Any>(
  val childCases: List<WorkflowOutputCase<*, *, StateT, OutputT>>,
  val workerCases: List<WorkerCase<*, StateT, OutputT>>,
  val nextActionFromEvent: Deferred<WorkflowAction<StateT, OutputT>>,
  val teardownHooks: List<() -> Unit>
) {

  // @formatter:off
  data class WorkflowOutputCase<
      ChildInputT : Any,
      ChildOutputT : Any,
      ParentStateT : Any,
      out ParentOutputT : Any
      >(
        val workflow: Workflow<*, ChildOutputT, *>,
        val id: WorkflowId<ChildInputT, ChildOutputT, *>,
        val input: ChildInputT,
        val handler: (ChildOutputT) -> WorkflowAction<ParentStateT, ParentOutputT>
      ) {
        @Suppress("UNCHECKED_CAST")
        fun acceptChildOutput(output: Any): WorkflowAction<ParentStateT, ParentOutputT> =
          handler(output as ChildOutputT)
      }
      // @formatter:on

  data class WorkerCase<T, StateT : Any, out OutputT : Any>(
    val worker: Worker<T>,
    val key: String,
    val handler: (OutputOrFinished<T>) -> WorkflowAction<StateT, OutputT>
  ) {
    @Suppress("UNCHECKED_CAST")
    fun acceptUpdate(value: OutputOrFinished<*>): WorkflowAction<StateT, OutputT> =
      handler(value as OutputOrFinished<T>)

    /** Override `equals` so this class can be used as its own key. */
    override fun equals(other: Any?): Boolean =
      other is WorkerCase<*, *, *> &&
          worker.doesSameWorkAs(other.worker) &&
          key == other.key

    /**
     * This object is used as a [Map] key, so it needs this method to obey the contract of returning
     * the same value for equivalent instances. Hardcoding this value to zero is not very efficient,
     * but since the total number of worker cases for any given workflow is unlikely to be very
     * large, degrading to a linear search (or whatever the map implementation happens to use for
     * non-equivalent keys with identical hashcodes) should be fine.
     */
    override fun hashCode(): Int = 0
  }
}
