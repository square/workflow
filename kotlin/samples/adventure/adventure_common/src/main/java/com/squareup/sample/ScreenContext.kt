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
package com.squareup.sample

import com.squareup.workflow.RenderContext
import com.squareup.workflow.WorkflowAction
import kotlin.experimental.ExperimentalTypeInference

/**
 * Helper to define a navigation backstack declaratively by rendering [ScreenWorkflow]s and
 * [SimpleScreenWorkflow]s.
 *
 * Returns as soon as a child [ScreenWorkflow] returns a [rendering][ScreenRendering] with a
 * null [GoBackHandler], or a child [SimpleScreenWorkflow] emits [NavigationOutput.GoBack].
 *
 * @param goBackAction The [WorkflowAction] to perform when the first child workflow wants to go
 * back.
 *
 * @return The rendering of the last child rendered, or the return value of [block] if all children
 * were rendered.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <S, O : Any, R> RenderContext<S, O>.renderScreensRoot(
  goBackAction: WorkflowAction<S, O>,
  @BuilderInference block: ScreenContext<S, O, R>.() -> R
): R = renderScreens(goBackAction, block).screenRendering

/**
 * Helper to define a navigation backstack declaratively by rendering [ScreenWorkflow]s and
 * [SimpleScreenWorkflow]s.
 *
 * Returns as soon as a child [ScreenWorkflow] returns a [rendering][ScreenRendering] with a
 * null [GoBackHandler], or a child [SimpleScreenWorkflow] emits [NavigationOutput.GoBack].
 *
 * @param goBackAction The [WorkflowAction] to perform when the first child workflow wants to go
 * back.
 *
 * @return A [ScreenRendering] that contains the rendering of the last child rendered, or the return
 * value of [block] if all children were rendered, and an optional [GoBackHandler] that will only
 * be non-null if all child workflows were finished and will cause the last workflow to be re-shown.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <S, O : Any, R> RenderContext<S, O>.renderScreens(
  goBackAction: WorkflowAction<S, O>,
  @BuilderInference block: ScreenContext<S, O, R>.() -> R
): ScreenRendering<R> {
  val goBackHandler = GoBackHandler(goBackAction.toString(), onEvent { goBackAction })
  return ScreenContext<S, O, R>(this, goBackHandler)
      .execute(block)
}

/**
 * Renders [ScreenWorkflow]s and [SimpleScreenWorkflow]s and plumbs [GoBackHandler]s between
 * workflows.
 */
class ScreenContext<S, O : Any, R> internal constructor(
  private val renderContext: RenderContext<S, O>,
  goBackHandler: GoBackHandler
) {

  /**
   * Thrown by [renderChild] to stop rendering any more children.
   */
  private class FinishRenderingException(val finalRendering: Any?) : Throwable() {
    // Don't care, so don't waste time/memory gathering stack frames.
    override fun fillInStackTrace(): Throwable = this
  }

  private var lastGoBackHandler: GoBackHandler = goBackHandler

  /**
   * Render a child [ScreenWorkflow] and return its rendering.
   *
   * @see RenderContext.renderChild
   */
  fun <CI, CO : Any> renderChild(
    workflow: ScreenWorkflow<CI, CO, R>,
    input: CI,
    key: String = "",
    handler: (CO) -> WorkflowAction<S, O>
  ): R {
    val r = renderContext.renderChild(workflow, ScreenInput(input, lastGoBackHandler), key, handler)
    if (r.goBackHandler == null) {
      // No goBackHandler means this workflow is the one that should be rendered, so we throw to
      // break out of the block passed to renderScreens early.
      throw FinishRenderingException(r.screenRendering)
    } else {
      lastGoBackHandler = r.goBackHandler
      return r.screenRendering
    }
  }

  /**
   * Executes [block] up until the first child workflow needs to render itself.
   */
  internal fun execute(
    block: ScreenContext<S, O, R>.() -> R
  ): ScreenRendering<R> {
    try {
      val r = block(this)
      return ScreenRendering(r, lastGoBackHandler)
    } catch (e: FinishRenderingException) {
      @Suppress("UNCHECKED_CAST")
      return ScreenRendering(e.finalRendering as R, goBackHandler = null)
    }
  }
}
