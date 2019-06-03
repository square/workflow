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

import com.squareup.workflow.Worker.OutputOrFinished
import com.squareup.workflow.Worker.OutputOrFinished.Finished
import com.squareup.workflow.Worker.OutputOrFinished.Output
import com.squareup.workflow.WorkflowAction.Companion.noop

/**
 * Facilities for a [Workflow] to interact with other [Workflow]s and the outside world from inside
 * a `render` function.
 *
 * ## Handling Events
 *
 * See [onEvent].
 *
 * ## Performing Asynchronous Work
 *
 * See [onWorkerOutput], [onWorkerOutputOrFinished], and [runningWorker].
 *
 * ## Composing Children
 *
 * See [renderChild].
 */
interface RenderContext<StateT, in OutputT : Any> {

  /**
   * Given a function that takes an [event][EventT] and can mutate the state or emit an output,
   * returns a function that will perform that workflow update when called with an event.
   * The returned function is valid until the next render pass.
   *
   * For example, if you have a rendering type of `Screen`:
   *
   *    data class Screen(
   *      val label: String,
   *      val onClick: () -> Unit
   *    )
   *
   * Then, from your `render` method, construct the screen like this:
   *
   *    return Screen(
   *      button1Label = "Hello",
   *      button2Label = "World",
   *      onClick = context.onEvent { buttonIndex ->
   *        emitOutput("Button $buttonIndex clicked!")
   *      }
   *    )
   *
   * ## Equivalence & Testing
   *
   * It is common in unit tests to get a rendering from a workflow, then (since renderings should be
   * value types) create the expected rendering instance and compare them. However, when comparing
   * renderings there is no meaningful way to compare event handlers (other than a null check if
   * they're nullable), and so two renderings that are identical but have different event handler
   * functions should still be considered equal. In order to support this pattern, the functions
   * returned by this method are all considered equal: `foo == bar` whenever `foo` and `bar` are
   * values returned by this method. More precisely, this function returns instances of
   * [EventHandler], and all [EventHandler] instances are considered equal.
   *
   * However, since event handling functions are always valid only for the rendering for which they
   * were created, this means that you can't dedup event handlers in production (e.g. with something
   * like RxJava's `distinctUntilChanged`). Event handlers must _always_ be updated, and if you
   * need to de-dup other view data, you must do it at a more granular level.
   *
   * @param handler A function that returns the [WorkflowAction] to perform when the event handler
   * is invoked.
   */
  fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): (EventT) -> Unit

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
  fun <ChildInputT, ChildOutputT : Any, ChildRenderingT> renderChild(
    child: Workflow<ChildInputT, ChildOutputT, ChildRenderingT>,
    input: ChildInputT,
    key: String = "",
    handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
  ): ChildRenderingT

  /**
   * Ensures [worker] is running. When the [Worker] emits an output or finishes, [handler] is called
   * to determine the [WorkflowAction] to take.
   *
   * @param key An optional string key that is used to distinguish between identical [Worker]s.
   */
  fun <T> onWorkerOutputOrFinished(
    worker: Worker<T>,
    key: String = "",
    handler: (OutputOrFinished<T>) -> WorkflowAction<StateT, OutputT>
  )
}

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take input.
 */
fun <StateT, OutputT : Any, ChildOutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, ChildOutputT, ChildRenderingT>,
      key: String = "",
      handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
    ): ChildRenderingT = renderChild(child, Unit, key, handler)
// @formatter:on

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take input or emit
 * output.
 */
fun <InputT, StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<InputT, Nothing, ChildRenderingT>,
      input: InputT,
      key: String = ""
    ): ChildRenderingT = renderChild(child, input, key) { noop() }
// @formatter:on

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take input or emit
 * output.
 */
fun <StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, Nothing, ChildRenderingT>,
      key: String = ""
    ): ChildRenderingT = renderChild(child, Unit, key) { noop() }
// @formatter:on

/**
 * Ensures [worker] is running. When the [Worker] emits an output, [handler] is called
 * to determine the [WorkflowAction] to take. When the worker finishes, nothing happens (although
 * another render pass may be triggered).
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
fun <StateT, OutputT : Any, T> RenderContext<StateT, OutputT>.onWorkerOutput(
  worker: Worker<T>,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onWorkerOutputOrFinished(worker, key) { outputOrFinished ->
  when (outputOrFinished) {
    is Output -> handler(outputOrFinished.value)
    Finished -> noop()
  }
}

/**
 * Ensures a [Worker] that never emits anything is running. Since [worker] can't emit anything,
 * it can't trigger any [WorkflowAction]s.
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
fun <StateT, OutputT : Any> RenderContext<StateT, OutputT>.runningWorker(
  worker: Worker<Nothing>,
  key: String = ""
) = onWorkerOutputOrFinished(worker, key) { throw AssertionError("Worker<Nothing> emitted $it") }
