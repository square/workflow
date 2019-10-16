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
import android.widget.FrameLayout
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.squareup.workflow.ui.BackStackAware.Companion.makeAware
import com.squareup.workflow.ui.BackStackConfig.First
import com.squareup.workflow.ui.BackStackConfig.Other
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.Named
import com.squareup.workflow.ui.R
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.bindShowRendering
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
) : FrameLayout(context, attributeSet, defStyle, defStyleRes) {

  private val viewStateCache = ViewStateCache()
  private lateinit var registry: ViewRegistry

  private val currentView: View? get() = if (childCount > 0) getChildAt(0) else null
  private var currentRendering: BackStackScreen<Named<*>>? = null

  private fun update(newRendering: BackStackScreen<*>) {
    val named: BackStackScreen<Named<*>> = newRendering.stack.asSequence()
        // Let interested children know that they're in a stack.
        .mapIndexed { index, frame ->
          val config = if (index == 0) First else Other
          makeAware(frame, config)
        }
        // ViewStateCache requires that everything be Named.
        // It's fine if client code is already using Named for its own purposes, recursion works.
        .map { Named(it, "backstack") }
        .toList()
        .let { BackStackScreen(it) }

    val oldViewMaybe = currentView

    // If existing view is compatible, just update it.
    oldViewMaybe
        ?.takeIf { it.canShowRendering(named.top) }
        ?.let {
          viewStateCache.prune(named.stack)
          it.showRendering(named.top)
          return
        }

    val newView = registry.buildView(named.top, this)
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
          val newScene = Scene(this, newView)

          val (outEdge, inEdge) = when (popped) {
            false -> Gravity.START to Gravity.END
            true -> Gravity.END to Gravity.START
          }

          val outSet = TransitionSet()
              .addTransition(Slide(outEdge).addTarget(oldView))
              .addTransition(Fade(Fade.OUT))

          val fullSet = TransitionSet()
              .addTransition(outSet)
              .addTransition(Slide(inEdge).excludeTarget(oldView, true))

          TransitionManager.go(newScene, fullSet)
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

  companion object : ViewBinding<BackStackScreen<*>>
  by BuilderBinding(
      type = BackStackScreen::class,
      viewConstructor = { viewRegistry, initialRendering, context, _ ->
        BackStackContainer(context)
            .apply {
              id = R.id.workflow_back_stack_container
              layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
              registry = viewRegistry
              bindShowRendering(initialRendering, ::update)
            }
      }
  )
}
