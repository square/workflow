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
@file:Suppress("DEPRECATION")

package com.squareup.workflow.internal

import com.squareup.workflow.EventHandler
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Sink
import com.squareup.workflow.Worker
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import kotlinx.coroutines.channels.SendChannel

/**
 * An implementation of [RenderContext] that builds a [Behavior] via [freeze].
 *
 * Not for general application use.
 */
class RealRenderContext<PropsT, StateT, OutputT : Any>(
  private val renderer: Renderer<PropsT, StateT, OutputT>,
  private val workerRunner: WorkerRunner<PropsT, StateT, OutputT>,
  private val eventActionsChannel: SendChannel<WorkflowAction<PropsT, StateT, OutputT>>
) : RenderContext<PropsT, StateT, OutputT>, Sink<WorkflowAction<PropsT, StateT, OutputT>> {

  interface Renderer<PropsT, StateT, OutputT : Any> {
    fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> render(
      child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
      props: ChildPropsT,
      key: String,
      handler: (ChildOutputT) -> WorkflowAction<PropsT, StateT, OutputT>
    ): ChildRenderingT
  }

  interface WorkerRunner<PropsT, StateT, OutputT : Any> {
    fun <T> runningWorker(
      worker: Worker<T>,
      key: String,
      handler: (T) -> WorkflowAction<PropsT, StateT, OutputT>
    )
  }

  /**
   * False during the current render call, set to true once this node is finished rendering.
   *
   * Used to:
   *  - prevent modifications to this object after [freeze] is called.
   *  - prevent sending to sinks before render returns.
   */
  private var frozen = false

  override val actionSink: Sink<WorkflowAction<PropsT, StateT, OutputT>> get() = this

  @Suppress("OverridingDeprecatedMember")
  override fun <EventT : Any> onEvent(handler: (EventT) -> WorkflowAction<PropsT, StateT, OutputT>):
      EventHandler<EventT> {
    checkNotFrozen()
    return EventHandler { event ->
      // Run the handler synchronously, so we only have to emit the resulting action and don't
      // need the update channel to be generic on each event type.
      val action = handler(event)
      eventActionsChannel.offer(action)
    }
  }

  override fun send(value: WorkflowAction<PropsT, StateT, OutputT>) {
    if (!frozen) {
      throw UnsupportedOperationException(
          "Expected sink to not be sent to until after the render pass. Received action: $value"
      )
    }
    eventActionsChannel.offer(value)
  }

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<PropsT, StateT, OutputT>
  ): ChildRenderingT {
    checkNotFrozen()
    return renderer.render(child, props, key, handler)
  }

  override fun <T> runningWorker(
    worker: Worker<T>,
    key: String,
    handler: (T) -> WorkflowAction<PropsT, StateT, OutputT>
  ) {
    checkNotFrozen()
    workerRunner.runningWorker(worker, key, handler)
  }

  /**
   * Freezes this context so that any further calls to this context will throw.
   */
  fun freeze() {
    checkNotFrozen()
    frozen = true
  }

  private fun checkNotFrozen() = check(!frozen) {
    "RenderContext cannot be used after render method returns."
  }
}
