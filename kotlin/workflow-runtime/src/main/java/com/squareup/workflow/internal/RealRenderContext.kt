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
import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.Behavior.WorkerCase
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import kotlinx.coroutines.CompletableDeferred

/**
 * An implementation of [RenderContext] that builds a [Behavior] via [buildBehavior].
 *
 * Not for general application use.
 */
class RealRenderContext<StateT, OutputT : Any>(
  private val renderer: Renderer<StateT, OutputT>
) : RenderContext<StateT, OutputT> {

  interface Renderer<StateT, in OutputT : Any> {
    fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> render(
      case: WorkflowOutputCase<ChildPropsT, ChildOutputT, StateT, OutputT>,
      child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
      id: WorkflowId<ChildPropsT, ChildOutputT, ChildRenderingT>,
      props: ChildPropsT
    ): ChildRenderingT
  }

  private val nextUpdateFromEvent = CompletableDeferred<WorkflowAction<StateT, OutputT>>()
  private val workerCases = mutableListOf<WorkerCase<*, StateT, OutputT>>()
  private val childCases = mutableListOf<WorkflowOutputCase<*, *, StateT, OutputT>>()

  /** Used to prevent modifications to this object after [buildBehavior] is called. */
  private var frozen = false

  override fun <EventT : Any> onEvent(handler: (EventT) -> WorkflowAction<StateT, OutputT>):
      EventHandler<EventT> {
    checkNotFrozen()
    return EventHandler { event ->
      // Run the handler synchronously, so we only have to emit the resulting action and don't
      // need the update channel to be generic on each event type.
      val update = handler(event)

      // If this returns false, we lost the race with another event being sent.
      check(nextUpdateFromEvent.complete(update)) {
        "Expected to successfully deliver event. Are you using an old rendering?\n" +
            "\tevent=$event\n" +
            "\tupdate=$update"
      }
    }
  }

  override fun <A : WorkflowAction<StateT, OutputT>> makeActionSink(): Sink<A> {
    checkNotFrozen()

    return object : Sink<A> {
      override fun send(value: A) {
        // If this returns false, we lost the race with another event being sent.
        check(nextUpdateFromEvent.complete(value)) {
          "Expected to successfully deliver $value. Are you using an old rendering?"
        }
      }
    }
  }

  override fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String,
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT {
    checkNotFrozen()
    val id = child.id(key)
    val case: WorkflowOutputCase<ChildPropsT, ChildOutputT, StateT, OutputT> =
      WorkflowOutputCase(child, id, props, handler)
    childCases += case
    return renderer.render(case, child, id, props)
  }

  override fun <T> runningWorkerUntilFinished(
    worker: Worker<T>,
    key: String,
    handler: (OutputOrFinished<T>) -> WorkflowAction<StateT, OutputT>
  ) {
    checkNotFrozen()
    workerCases += WorkerCase(worker, key, handler)
  }

  /**
   * Constructs an immutable [Behavior] from the context.
   */
  fun buildBehavior(): Behavior<StateT, OutputT> {
    checkNotFrozen()
    frozen = true
    return Behavior(
        childCases = childCases.toList(),
        workerCases = workerCases.toList(),
        nextActionFromEvent = nextUpdateFromEvent
    )
  }

  private fun checkNotFrozen() = check(!frozen) {
    "RenderContext cannot be used after render method returns."
  }
}
