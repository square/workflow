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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * A placeholder [View] that can replace itself with ones driven by workflow renderings,
 * similar to [android.view.ViewStub].
 *
 * ## Usage
 *
 * In the XML layout for a container view, place a [WorkflowViewStub] where
 * you want child renderings to be displayed. E.g.:
 *
 *    <LinearLayout…>
 *
 *        <com.squareup.workflow.ui.WorkflowViewStub
 *            android:id="@+id/child_stub"
 *            />
 *       …
 *
 * Then in your [LayoutRunner],
 *   - pull the view out with `findViewById` like any other view
 *   - and update it in your `showRendering` method:
 *
 * ```
 *     class YourLayoutRunner(view: View) {
 *       private val child = view.findViewById<WorkflowViewStub>(R.id.child_stub)
 *
 *       override fun showRendering(
 *          rendering: YourRendering,
 *          viewEnvironment: ViewEnvironment
 *       ) {
 *         child.update(rendering.childRendering, viewEnvironment)
 *       }
 *     }
 * ```
 */
class WorkflowViewStub @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : View(context, attributeSet, defStyle, defStyleRes) {
  init {
    setWillNotDraw(true)
  }

  /**
   * On-demand access to the delegate established by the last call to [update],
   * or this [WorkflowViewStub] instance if  none has yet been set.
   */
  var actual: View = this

  /**
   * Replaces this view with one that can display [rendering]. If the receiver
   * has already been replaced, updates the replacement if it [canShowRendering].
   * If the current replacement can't handle [rendering], a new view is put in place.
   *
   * @return the view that showed [rendering]
   *
   * @throws IllegalArgumentException if no binding can be find for the type of [rendering]
   *
   * @throws IllegalStateException if the matching [ViewFactory] fails to call
   * [View.bindShowRendering] when constructing the view
   */
  fun update(
    rendering: Any,
    viewEnvironment: ViewEnvironment
  ): View {
    actual.takeIf { it.canShowRendering(rendering) }
        ?.let {
          it.showRendering(rendering, viewEnvironment)
          return it
        }

    return when (val parent = actual.parent) {
      is ViewGroup -> viewEnvironment[ViewRegistry].buildView(rendering, viewEnvironment, parent)
          .also { buildNewViewAndReplaceOldView(parent, it) }
      else -> viewEnvironment[ViewRegistry].buildView(rendering, viewEnvironment, actual.context)
    }.also { actual = it }
  }

  private fun buildNewViewAndReplaceOldView(
    parent: ViewGroup,
    newView: View
  ) {
    val index = parent.indexOfChild(actual)
    parent.removeView(actual)

    actual.layoutParams
        ?.let { parent.addView(newView, index, it) }
        ?: run { parent.addView(newView, index) }
  }
}
