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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow

import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.WorkflowAction.Updater

/**
 * Facilities for a [Workflow] to interact with other [Workflow]s and the outside world from inside
 * a `render` function.
 *
 * ## Handling Events
 *
 * See [makeActionSink].
 *
 * ## Performing Asynchronous Work
 *
 * See [runningWorker].
 *
 * ## Composing Children
 *
 * See [renderChild].
 */
interface RenderContext<StateT, OutputT : Any> {

  @Deprecated("Use makeActionSink.")
  fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): (EventT) -> Unit

  /**
   * Creates a sink that will accept a single [WorkflowAction] of the given type.
   * Invokes that action by calling [WorkflowAction.apply] to update the current
   * state, and optionally emits the returned output value if it is non-null.
   *
   * Note that only a single action can be processed by the sink (or sinks) created
   * during a `render` call. Redundant calls to [Sink.send] will result in exceptions
   * being thrown.
   */
  fun <A : WorkflowAction<StateT, OutputT>> makeActionSink(): Sink<A>

  /**
   * Ensures [child] is running as a child of this workflow, and returns the result of its
   * `render` method.
   *
   * **Never call [StatefulWorkflow.render] or [StatelessWorkflow.render] directly, always do it
   * through this context method.**
   *
   * 1. If the child _wasn't_ already running, it will be started either from
   *    [initialState][Workflow.initialState] or its snapshot.
   * 2. If the child _was_ already running, The workflow's
   *    [onInputChanged][StatefulWorkflow.onInputChanged] method is invoked with the previous input
   *    and this one.
   * 3. The child's `render` method is invoked with `input` and the child's state.
   *
   * After this method returns, if something happens that trigger's one of `child`'s handlers, and
   * that handler emits an output, the function passed as [handler] will be invoked with that
   * output.
   *
   * @param key An optional string key that is used to distinguish between workflows of the same
   * type.
   */
  fun <ChildPropsT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildPropsT, ChildOutputT, ChildRenderingT>,
    props: ChildPropsT,
    key: String = "",
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT

  /**
   * Ensures [worker] is running. When the [Worker] emits an output, [handler] is called
   * to determine the [WorkflowAction] to take. When the worker finishes, nothing happens (although
   * another render pass may be triggered).
   *
   * @param key An optional string key that is used to distinguish between identical [Worker]s.
   */
  fun <T> runningWorker(
    worker: Worker<T>,
    key: String = "",
    handler: (T) -> WorkflowAction<StateT, OutputT>
  )
}

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props.
 */
/* ktlint-disable parameter-list-wrapping */
fun <StateT, OutputT : Any, ChildOutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<Unit, ChildOutputT, ChildRenderingT>,
  key: String = "",
  handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
): ChildRenderingT = renderChild(child, Unit, key, handler)
/* ktlint-enable parameter-list-wrapping */

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props or emit
 * output.
 */
/* ktlint-disable parameter-list-wrapping */
fun <PropsT, StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<PropsT, Nothing, ChildRenderingT>,
  props: PropsT,
  key: String = ""
): ChildRenderingT = renderChild(child, props, key) { noAction() }
/* ktlint-enable parameter-list-wrapping */

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props or emit
 * output.
 */
/* ktlint-disable parameter-list-wrapping */
fun <StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<Unit, Nothing, ChildRenderingT>,
  key: String = ""
): ChildRenderingT = renderChild(child, Unit, key) { noAction() }
/* ktlint-enable parameter-list-wrapping */

/**
 * Ensures a [Worker] that never emits anything is running. Since [worker] can't emit anything,
 * it can't trigger any [WorkflowAction]s.
 *
 * A simple way to create workers that don't output anything is using [Worker.createSideEffect].
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
fun <StateT, OutputT : Any> RenderContext<StateT, OutputT>.runningWorker(
  worker: Worker<Nothing>,
  key: String = ""
) {
  // Need to cast to Any so the compiler doesn't complain about unreachable code.
  runningWorker(worker as Worker<Any>, key) {
    throw AssertionError("Worker<Nothing> emitted $it")
  }
}

/**
 * Alternative to [RenderContext.makeActionSink] that allows externally defined
 * event types to be mapped to anonymous [WorkflowAction]s.
 */
fun <EventT, StateT, OutputT : Any> RenderContext<StateT, OutputT>.makeEventSink(
  update: Updater<StateT, OutputT>.(EventT) -> Unit
): Sink<EventT> {
  val actionSink = makeActionSink<WorkflowAction<StateT, OutputT>>()

  return actionSink.contraMap { event ->
    WorkflowAction({ "eventSink($event)" }) { update(event) }
  }
}

/**
 * Ensures [worker] is running. When the [Worker] emits an output, [handler] is called
 * to determine the [WorkflowAction] to take. When the worker finishes, nothing happens (although
 * another render pass may be triggered).
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
@Deprecated(
    "Use runningWorker",
    ReplaceWith("runningWorker(worker, key, handler)", "com.squareup.workflow.runningWorker")
)
fun <StateT, OutputT : Any, T> RenderContext<StateT, OutputT>.onWorkerOutput(
  worker: Worker<T>,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = runningWorker(worker, key, handler)
