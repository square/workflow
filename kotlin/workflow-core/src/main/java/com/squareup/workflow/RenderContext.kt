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
interface RenderContext<StateT : Any, in OutputT : Any> {

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
   * @param handler A function that returns the [WorkflowAction] to perform when the event handler
   * is invoked.
   */
  fun <EventT : Any> onEvent(
    handler: (EventT) -> WorkflowAction<StateT, OutputT>
  ): EventHandler<EventT>

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
  fun <ChildInputT : Any, ChildOutputT : Any, ChildRenderingT : Any> renderChild(
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

  /**
   * Adds an action to be invoked if the workflow is discarded by its parent before the next
   * render pass. Multiple hooks can be registered in the same render pass, they will be invoked
   * in the same order they're set. Like any other work performed through the context (e.g. calls
   * to [renderChild] or [onWorkerOutput]), hooks are cleared at the start of each render pass, so
   * you must set any hooks you need in each render pass.
   *
   * Teardown handlers should be non-blocking and execute quickly, since they are invoked
   * synchronously during the render pass.
   */
  @Deprecated("Use the CoroutineScope parameter to initialState.")
  fun onTeardown(handler: () -> Unit)
}

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take input.
 */
fun <StateT : Any, OutputT : Any, ChildOutputT : Any, ChildRenderingT : Any>
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
fun <InputT : Any, StateT : Any, OutputT : Any, ChildRenderingT : Any>
    RenderContext<StateT, OutputT>.renderChild(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<InputT, Nothing, ChildRenderingT>,
      input: InputT,
      key: String = ""
    ): ChildRenderingT = renderChild(child, input, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take input or emit
 * output.
 */
fun <StateT : Any, OutputT : Any, ChildRenderingT : Any>
    RenderContext<StateT, OutputT>.renderChild(
// Intellij refuses to format this parameter list correctly because of the weird line break,
// and detekt will complain about it.
// @formatter:off
      child: Workflow<Unit, Nothing, ChildRenderingT>,
      key: String = ""
    ): ChildRenderingT = renderChild(child, Unit, key) { WorkflowAction.noop() }
// @formatter:on

/**
 * Ensures [worker] is running. When the [Worker] emits an output, [handler] is called
 * to determine the [WorkflowAction] to take. If the [Worker] finishes without emitting anything,
 * throws a [NoSuchElementException].
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 * @throws NoSuchElementException If the worker finished without emitting a value.
 */
fun <StateT : Any, OutputT : Any, T> RenderContext<StateT, OutputT>.onWorkerOutput(
  worker: Worker<T>,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = onWorkerOutputOrFinished(worker, key) { outputOrFinished ->
  when (outputOrFinished) {
    is Output -> handler(outputOrFinished.value)
    Finished -> {
      throw NoSuchElementException("Expected Worker (key=$key) to emit an output: $worker")
    }
  }
}

/**
 * Ensures a [Worker] that never emits anything is running. Since [worker] can't emit anything,
 * it can't trigger any [WorkflowAction]s.
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
fun <StateT : Any, OutputT : Any> RenderContext<StateT, OutputT>.runningWorker(
  worker: Worker<Nothing>,
  key: String = ""
) = onWorkerOutputOrFinished(worker, key) { throw AssertionError("Worker<Nothing> emitted $it") }
