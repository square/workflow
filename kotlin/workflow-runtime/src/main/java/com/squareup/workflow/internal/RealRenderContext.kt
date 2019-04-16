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

import com.squareup.workflow.EventHandler
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Worker
import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Workflow
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.internal.Behavior.WorkerCase
import com.squareup.workflow.internal.Behavior.WorkflowOutputCase
import kotlinx.coroutines.CompletableDeferred

/**
 * An implementation of [RenderContext] that builds a [Behavior] via [buildBehavior].
 */
internal class RealRenderContext<StateT : Any, OutputT : Any>(
  private val renderer: Renderer<StateT, OutputT>
) : RenderContext<StateT, OutputT> {

  interface Renderer<StateT : Any, in OutputT : Any> {
    fun <ChildInputT : Any, ChildOutputT : Any, ChildRenderingT : Any> render(
      case: WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT>,
      child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
      id: WorkflowId<ChildInputT, ChildOutputT, ChildRenderingT>,
      input: ChildInputT
    ): ChildRenderingT
  }

  private val nextUpdateFromEvent = CompletableDeferred<WorkflowAction<StateT, OutputT>>()
  private val workerCases = mutableListOf<WorkerCase<*, StateT, OutputT>>()
  private val childCases = mutableListOf<WorkflowOutputCase<*, *, StateT, OutputT>>()
  private val teardownHooks = mutableListOf<() -> Unit>()

  /** Used to prevent modifications to this object after [buildBehavior] is called. */
  private var frozen = false

  override fun <EventT : Any> onEvent(handler: (EventT) -> WorkflowAction<StateT, OutputT>):
      EventHandler<EventT> {
    checkNotFrozen()
    return EventHandler { event ->
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

  // @formatter:off
  override fun <ChildInputT : Any, ChildOutputT : Any, ChildRenderingT : Any>
      renderChild(
        child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
        input: ChildInputT,
        key: String,
        handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
      ): ChildRenderingT {
    // @formatter:on
    checkNotFrozen()
    val id = child.id(key)
    val case: WorkflowOutputCase<ChildInputT, ChildOutputT, StateT, OutputT> =
      WorkflowOutputCase(child, id, input, handler)
    childCases += case
    return renderer.render(case, child, id, input)
  }

  override fun <T> onWorkerOutputOrFinished(
    worker: Worker<T>,
    key: String,
    handler: (OutputOrFinished<T>) -> WorkflowAction<StateT, OutputT>
  ) {
    checkNotFrozen()
    workerCases += WorkerCase(worker, key, handler)
  }

  override fun onTeardown(handler: () -> Unit) {
    checkNotFrozen()
    teardownHooks += handler
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
        nextActionFromEvent = nextUpdateFromEvent,
        teardownHooks = teardownHooks.toList()
    )
  }

  private fun checkNotFrozen() = check(!frozen) {
    "RenderContext cannot be used after render method returns."
  }
}
