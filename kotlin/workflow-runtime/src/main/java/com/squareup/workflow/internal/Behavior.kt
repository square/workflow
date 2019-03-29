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

import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.util.ChannelUpdate
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlin.reflect.KType

/**
 * An immutable description of the things a [Workflow] would like to do as the result of calling its
 * `compose` method. A `Behavior` is built up by calling methods on a
 * [WorkflowContext][com.squareup.workflow.WorkflowContext] ([RealWorkflowContext] in particular).
 *
 * @see RealWorkflowContext
 */
internal data class Behavior<StateT : Any, out OutputT : Any>(
  val childCases: List<WorkflowOutputCase<*, *, StateT, OutputT>>,
  val subscriptionCases: List<SubscriptionCase<*, StateT, OutputT>>,
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

  data class SubscriptionCase<E, StateT : Any, out OutputT : Any>(
    val channelProvider: CoroutineScope.() -> ReceiveChannel<E>,
    val idempotenceKey: Pair<KType, String>,
    val handler: (ChannelUpdate<E>) -> WorkflowAction<StateT, OutputT>
  ) {
    @Suppress("UNCHECKED_CAST")
    fun acceptUpdate(value: ChannelUpdate<*>): WorkflowAction<StateT, OutputT> =
      handler(value as ChannelUpdate<E>)
  }
}
