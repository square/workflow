/*
 * Copyright 2018 Square Inc.
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
package com.squareup.workflow.ui.backstack

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.transition.Scene
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.Named
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.backstack.BackStackConfig.First
import com.squareup.workflow.ui.backstack.BackStackConfig.Other
import com.squareup.workflow.ui.bindShowRendering
import com.squareup.workflow.ui.buildView
import com.squareup.workflow.ui.canShowRendering
import com.squareup.workflow.ui.compatible
import com.squareup.workflow.ui.showRendering

/**
 * A container view that can display a stream of [BackStackScreen] instances.
 */
open class BackStackContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : ViewGroup(context, attributeSet, defStyle, defStyleRes) {

  private val viewStateCache = ViewStateCache()

  private val currentView: View? get() = if (childCount > 0) getChildAt(0) else null
  private var currentRendering: BackStackScreen<Named<*>>? = null

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    currentView?.layout(0, 0, measuredWidth, measuredHeight)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    currentView?.measure(widthMeasureSpec, heightMeasureSpec)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  protected fun update(
    newRendering: BackStackScreen<*>,
    newViewEnvironment: ViewEnvironment
  ) {
    val config = if (newRendering.backStack.isEmpty()) First else Other
    val environment = newViewEnvironment + (BackStackConfig to config)

    val named: BackStackScreen<Named<*>> = newRendering
        // ViewStateCache requires that everything be Named.
        // It's fine if client code is already using Named for its own purposes, recursion works.
        .map { Named(it, "backstack") }

    val oldViewMaybe = currentView

    // If existing view is compatible, just update it.
    oldViewMaybe
        ?.takeIf { it.canShowRendering(named.top) }
        ?.let {
          viewStateCache.prune(named.frames)
          it.showRendering(named.top, environment)
          return
        }

    val newView = environment[ViewRegistry].buildView(named.top, environment, this)
    viewStateCache.update(named.backStack, oldViewMaybe, newView)

    val popped = currentRendering?.backStack?.any { compatible(it, named.top) } == true

    performTransition(oldViewMaybe, newView, popped)
    currentRendering = named
  }

  /**
   * Called from [View.showRendering] to swap between views.
   * Subclasses can override to customize visual effects. There is no need to call super.
   * Note that views are showing renderings of type [Named]`<BackStackScreen<*>>`.
   *
   * @param oldViewMaybe the outgoing view, or null if this is the initial rendering.
   * @param newView the view that should replace [oldViewMaybe] (if it exists), and become
   * this view's only child
   * @param popped true if we should give the appearance of popping "back" to a previous rendering,
   * false if a new rendering is being "pushed". Should be ignored if [oldViewMaybe] is null.
   */
  protected open fun performTransition(
    oldViewMaybe: View?,
    newView: View,
    popped: Boolean
  ) {
    // Showing something already, transition with push or pop effect.
    oldViewMaybe
        ?.let { oldView ->
          val oldBody: View? = oldView.findViewById(R.id.back_stack_body)
          val newBody: View? = newView.findViewById(R.id.back_stack_body)

          val oldTarget: View
          val newTarget: View
          if (oldBody != null && newBody != null) {
            oldTarget = oldBody
            newTarget = newBody
          } else {
            oldTarget = oldView
            newTarget = newView
          }

          val (outEdge, inEdge) = when (popped) {
            false -> Gravity.START to Gravity.END
            true -> Gravity.END to Gravity.START
          }

          val transition = TransitionSet()
              .addTransition(Slide(outEdge).addTarget(oldTarget))
              .addTransition(Slide(inEdge).addTarget(newTarget))
              .setInterpolator(AccelerateDecelerateInterpolator())

          TransitionManager.go(Scene(this, newView), transition)
          return
        }

    // This is the first view, just show it.
    addView(newView)
  }

  override fun onSaveInstanceState(): Parcelable {
    return ViewStateCache.SavedState(super.onSaveInstanceState(), viewStateCache)
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? ViewStateCache.SavedState)
        ?.let {
          viewStateCache.restore(it.viewStateCache)
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  companion object : ViewFactory<BackStackScreen<*>>
  by BuilderBinding(
      type = BackStackScreen::class,
      viewConstructor = { initialRendering, initialHints, context, _ ->
        BackStackContainer(context)
            .apply {
              id = R.id.workflow_back_stack_container
              layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
              bindShowRendering(initialRendering, initialHints, ::update)
            }
      }
  )
}
