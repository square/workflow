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

import android.view.View

/**
 * Function attached to a view created by [ViewRegistry], to allow it
 * to respond to [View.showRendering].
 */
typealias ViewShowRendering<RenderingT> = (@UnsafeVariance RenderingT, ContainerHints) -> Unit

/**
` * View tag that holds the function to make the view show instances of [RenderingT], and
 * the [current rendering][showing].
 *
 * @param showing the current rendering. Used by [canShowRendering] to decide if the
 * view can be updated with the next rendering.
 */
data class ShowRenderingTag<out RenderingT : Any>(
  val showing: RenderingT,
  val showRendering: ViewShowRendering<RenderingT>
)

/**
 * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
 *
 * Establishes [showRendering] as the implementation of [View.showRendering]
 * for the receiver, possibly replacing the existing one. Immediately invokes [showRendering]
 * to display [initialRendering].
 *
 * Intended for use by implementations of [ViewBinding.buildView].
 */
fun <RenderingT : Any> View.bindShowRendering(
  initialRendering: RenderingT,
  initialContainerHints: ContainerHints,
  showRendering: ViewShowRendering<RenderingT>
) {
  setTag(R.id.view_show_rendering_function, ShowRenderingTag(initialRendering, showRendering))
  showRendering.invoke(initialRendering, initialContainerHints)
}

/**
 * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
 *
 * True if this view is able to show [rendering].
 *
 * Returns `false` if [bindShowRendering] has not been called, so it is always safe to
 * call this method. Otherwise returns the [compatibility][compatible] of the initial
 * [rendering] and the new one.
 */
fun View.canShowRendering(rendering: Any): Boolean {
  return getRendering<Any>()?.matches(rendering) == true
}

/**
 * It is usually more convenient to use [WorkflowViewStub] than to call this method directly.
 *
 * Sets the workflow rendering associated with this view, and displays it by
 * invoking the [ViewShowRendering] function previously set by [bindShowRendering].
 *
 * @throws IllegalStateException if [bindShowRendering] has not been called.
 */
fun <RenderingT : Any> View.showRendering(
  rendering: RenderingT,
  containerHints: ContainerHints
) {
  showRenderingTag
      ?.let { tag ->
        check(tag.showing.matches(rendering)) {
          "Expected $this to be able to show rendering $rendering, but that did not match " +
              "previous rendering ${tag.showing}. " +
              "Consider using ${WorkflowViewStub::class.java.simpleName} to display arbitrary types."
        }

        bindShowRendering(rendering, containerHints, tag.showRendering)
      }
      ?: error(
          "Expected $this to have a showRendering function to show $rendering. " +
              "Perhaps it was not built by a ${ViewRegistry::class.java.simpleName}, " +
              "or perhaps its ${ViewBinding::class.java.simpleName} did not call" +
              "View.bindShowRendering."
      )
}

/**
 * Returns the most recent rendering shown by this view, or null if [bindShowRendering]
 * has never been called.
 */
fun <RenderingT : Any> View.getRendering(): RenderingT? {
  @Suppress("UNCHECKED_CAST")
  return when (val showing = showRenderingTag?.showing) {
    null -> null
    else -> showing as RenderingT
  }
}

/**
 * Returns the function set by the most recent call to [bindShowRendering], or null
 * if that method has never been called.
 */
fun <RenderingT : Any> View.getShowRendering(): ViewShowRendering<RenderingT>? {
  // IDE is lying, casting here is unnecessary and causes a compiler warning.
  // https://youtrack.jetbrains.com/issue/KT-34433
  return showRenderingTag?.showRendering
}

/**
 * Returns the [ShowRenderingTag] established by the last call to [View.bindShowRendering],
 * or null if that method has never been called.
 */
@PublishedApi
internal val View.showRenderingTag: ShowRenderingTag<*>?
  get() = getTag(R.id.view_show_rendering_function) as? ShowRenderingTag<*>

private fun Any.matches(other: Any) = compatible(this, other)
