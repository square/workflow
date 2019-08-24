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
package com.squareup.workflow.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KClass

/**
 * A [ViewBinding] that allows [ViewRegistry.buildView] to dispense [View]s that need
 * to be generated from code. (Use [LayoutRunner] to work with XML layout resources.)
 *
 * Typical usage is to have a custom builder or view's `companion object` implement
 * [ViewBinding] by delegating to a [BuilderBinding], like this:
 *
 *    class MyView(
 *      context: Context,
 *      attributeSet: AttributeSet?
 *    ) : FrameLayout(context, attributeSet) {
 *      private fun update(rendering:  MyRendering) { ... }
 *
 *      companion object : ViewBuilder<MyScreen>
 *      by BuilderBinding(
 *          type = MyScreen::class,
 *          builder = { _, initialRendering, context, _ ->
 *            MyView(context).apply {
 *              layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
 *              bindShowRendering(initialRendering, ::update)
 *            }
 *      )
 *    }
 *
 * This pattern allows us to assemble a [ViewRegistry] out of the
 * custom classes themselves.
 *
 *    val TicTacToeViewBuilders = ViewRegistry(
 *        MyView, GamePlayLayoutRunner, GameOverLayoutRunner
 *    )
 *
 * Note in particular the [ViewRegistry] argument to the [viewConstructor] lambda. This allows
 * nested renderings to be displayed via nested calls to [ViewRegistry.buildView].
 */
class BuilderBinding<RenderingT : Any>(
  override val type: KClass<RenderingT>,
  private val viewConstructor: (
    viewRegistry: ViewRegistry,
    initialRendering: RenderingT,
    contextForNewView: Context,
    container: ViewGroup?
  ) -> View
) : ViewBinding<RenderingT> {
  override fun buildView(
    registry: ViewRegistry,
    initialRendering: RenderingT,
    contextForNewView: Context,
    container: ViewGroup?
  ): View = viewConstructor.invoke(registry, initialRendering, contextForNewView, container)
}
