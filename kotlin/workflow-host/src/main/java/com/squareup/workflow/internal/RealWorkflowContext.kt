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

import com.squareup.workflow.ChannelUpdate
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.WorkflowContext
import com.squareup.workflow.internal.Behavior.SubscriptionCase
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * An implementation of [WorkflowContext] that builds a [Behavior] via [buildBehavior].
 */
internal class RealWorkflowContext<StateT : Any, OutputT : Any>(
  private val composer: Composer<StateT, OutputT>
) : WorkflowContext<StateT, OutputT> {

  interface Composer<StateT : Any, in OutputT : Any> {
    fun <ChildInputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any> compose(
      case: WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT>,
      child: Workflow<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
      id: WorkflowId<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
      input: ChildInputT
    ): ChildRenderingT
  }

  private val nextUpdateFromEvent = CompletableDeferred<WorkflowAction<StateT, OutputT>>()
  private val subscriptionCases = mutableListOf<SubscriptionCase<*, StateT, OutputT>>()
  private val childCases = mutableListOf<WorkflowOutputCase<*, *, StateT, OutputT>>()

  override fun <EventT : Any> makeSink(handler: (EventT) -> WorkflowAction<StateT, OutputT>):
      (EventT) -> Unit {
    return { event ->
      // Run the handler synchronously, so we only have to emit the resulting action and don't need the
      // update channel to be generic on each event type.
      val update = handler(event)

      // If this returns false, we lost the race with another event being sent.
      check(nextUpdateFromEvent.complete(update)) {
        "Expected to successfully deliver event. Are you using an old rendering?\n" +
            "\tevent=$event\n" +
            "\tupdate=$update"
      }
    }
  }

  override fun <E> onReceiveRaw(
    channelProvider: CoroutineScope.() -> ReceiveChannel<E>,
    idempotenceKey: Any,
    handler: (ChannelUpdate<E>) -> WorkflowAction<StateT, OutputT>
  ) {
    subscriptionCases += SubscriptionCase(
        channelProvider,
        idempotenceKey,
        handler
    )
  }

  // @formatter:off
  override fun <ChildInputT : Any, ChildStateT : Any, ChildOutputT : Any, ChildRenderingT : Any>
      compose(
        child: Workflow<ChildInputT, ChildStateT, ChildOutputT, ChildRenderingT>,
        input: ChildInputT,
        key: String,
        handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
      ): ChildRenderingT {
  // @formatter:on
    val id = child.id(key)
    val case: WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT> =
      WorkflowOutputCase(child, id, input, handler)
    childCases += case
    return composer.compose(case, child, id, input)
  }

  /**
   * Constructs an immutable [Behavior] from the context.
   */
  fun buildBehavior(): Behavior<StateT, OutputT> = Behavior(
      childCases = childCases.toList(),
      subscriptionCases = subscriptionCases.toList(),
      nextActionFromEvent = nextUpdateFromEvent
  )
}
