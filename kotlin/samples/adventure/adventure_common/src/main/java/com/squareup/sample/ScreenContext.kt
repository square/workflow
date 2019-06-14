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
import com.squareup.workflow.ui.BackStackScreen
import kotlin.experimental.ExperimentalTypeInference

/**
 * Helper to define a navigation backstack declaratively by rendering [ScreenWorkflow]s and
 * [SimpleScreenWorkflow]s.
 *
 * Returns as soon as a child [ScreenWorkflow] returns a [rendering][ScreenRendering] with a
 * null [GoBackHandler].
 *
 * @param goBackAction The [WorkflowAction] to perform when the first child workflow wants to go
 * back.
 *
 * @return A [ScreenRendering] that contains the rendering of the last child rendered, or the return
 * value of [block] if all children were rendered, and an optional [GoBackHandler] that will only
 * be non-null if all child workflows were finished and will cause the last workflow to be re-shown.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <S, O : Any, R : Any, D : Any> RenderContext<S, O>.renderScreens(
  goBackAction: WorkflowAction<S, O>,
  @BuilderInference block: ScreenContext<S, O, D>.() -> R
): ScreenRendering<R?, BackStackScreen<D>> {
  val goBackHandler = GoBackHandler(goBackAction.toString(), onEvent { goBackAction })
  return renderScreens(goBackHandler, block)
}

/**
 * Helper to define a navigation backstack declaratively by rendering [ScreenWorkflow]s and
 * [SimpleScreenWorkflow]s.
 *
 * Returns as soon as a child [ScreenWorkflow] returns a [rendering][ScreenRendering] with a
 * null [GoBackHandler].
 *
 * @return A [ScreenRendering] that contains the rendering of the last child rendered, or the return
 * value of [block] if all children were rendered, and an optional [GoBackHandler] that will only
 * be non-null if all child workflows were finished and will cause the last workflow to be re-shown.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <S, O : Any, R : Any, D : Any> RenderContext<S, O>.renderScreens(
  goBackHandler: GoBackHandler,
  @BuilderInference block: ScreenContext<S, O, D>.() -> R
): ScreenRendering<R?, BackStackScreen<D>> {
  return ScreenContext<S, O, D>(this, goBackHandler)
      .execute(block)
}

/**
 * Renders [ScreenWorkflow]s and [SimpleScreenWorkflow]s and plumbs [GoBackHandler]s between
 * workflows.
 */
class ScreenContext<S, O : Any, D : Any> internal constructor(
  private val renderContext: RenderContext<S, O>,
  goBackHandler: GoBackHandler
) {

  /**
   * Thrown by [renderChild] to stop rendering any more children.
   */
  private class FinishRenderingException(val backstack: List<Any?>) : Throwable() {
    // Don't care, so don't waste time/memory gathering stack frames.
    override fun fillInStackTrace(): Throwable = this
  }

  private var lastGoBackHandler: GoBackHandler = goBackHandler
  private val backstack = mutableListOf<D>()

  /**
   * Render a child [ScreenWorkflow] and return its rendering.
   *
   * @see RenderContext.renderChild
   */
  fun <CI, CO : Any, R> renderChild(
    workflow: ScreenWorkflow<CI, CO, R, D>,
    input: CI,
    key: String = "",
    handler: (CO) -> WorkflowAction<S, O>
  ): R {
    val r = renderContext.renderChild(workflow, ScreenInput(input, lastGoBackHandler), key, handler)
    backstack += r.display

    if (r.goBackHandler == null) {
      // No goBackHandler means this workflow is the one that should be rendered, so we throw to
      // break out of the block passed to renderScreens early.
      throw FinishRenderingException(backstack)
    } else {
      lastGoBackHandler = r.goBackHandler
      return r.screenRendering
    }
  }

  fun <CI, CO : Any, R> renderBackstack(
    workflow: ScreenWorkflow<CI, CO, R, BackStackScreen<D>>,
    input: CI,
    key: String = "",
    handler: (CO) -> WorkflowAction<S, O>
  ): R {
    val r = renderContext.renderChild(workflow, ScreenInput(input, lastGoBackHandler), key, handler)
    backstack.addAll(r.display.stack)

    if (r.goBackHandler == null) {
      // No goBackHandler means this workflow is the one that should be rendered, so we throw to
      // break out of the block passed to renderScreens early.
      throw FinishRenderingException(backstack)
    } else {
      // Back navigation should be delegated to the sub-flow, so use the backstack's back handler
      // instead of the ScreenRendering's.
      lastGoBackHandler = GoBackHandler("todo") { r.display.onGoBack }
      return r.screenRendering
    }
  }

  /**
   * Executes [block] up until the first child workflow needs to render itself.
   */
  internal fun <R : Any> execute(
    block: ScreenContext<S, O, D>.() -> R
  ): ScreenRendering<R?, BackStackScreen<D>> {
    try {
      val finalRendering = block(this)
      return ScreenRendering(
          screenRendering = finalRendering,
          display = BackStackScreen(
              stack = backstack.toList(),
              onGoBack = lastGoBackHandler
          ),
          goBackHandler = lastGoBackHandler
      )
    } catch (e: FinishRenderingException) {
      @Suppress("UNCHECKED_CAST")
      return ScreenRendering(
          screenRendering = null,
          display = BackStackScreen(
              stack = (e.backstack as List<D>).toList(),
              onGoBack = lastGoBackHandler
          ),
          // This particular "subflow" isn't finished rendering, so there's no top-level back
          // handler.
          goBackHandler = null
      )
    }
  }
}
