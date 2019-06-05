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
import android.support.transition.Fade
import android.support.transition.Scene
import android.support.transition.Slide
import android.support.transition.TransitionManager
import android.support.transition.TransitionSet
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.HandlesBack
import com.squareup.workflow.ui.R
import com.squareup.workflow.ui.Named
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.backstack.ViewStateCache.SavedState
import com.squareup.workflow.ui.bindShowRendering
import com.squareup.workflow.ui.canShowRendering
import com.squareup.workflow.ui.showRendering

/**
 * A container view that can display a stream of [BackStackScreen] instances.
 *
 * This view is back button friendly -- it implements [HandlesBack], delegating
 * to displayed views that implement that interface themselves.
 */
@ExperimentalWorkflowUi
open class BackStackContainer(
  context: Context,
  attributeSet: AttributeSet?
) : FrameLayout(context, attributeSet), HandlesBack {
  constructor(context: Context) : this(context, null)

  private var restored: ViewStateCache? = null
  private val viewStateCache by lazy { restored ?: ViewStateCache() }

  private val showing: View? get() = if (childCount > 0) getChildAt(0) else null

  private lateinit var registry: ViewRegistry

  private fun update(newRendering: BackStackScreen<*>) {
    // ViewStateCache requires that everything be Named, for ease of comparison and
    // serialization (that Named.key string is very handy). It's fine if client code is
    // already using Named for its own purposes, recursion works.
    val named: BackStackScreen<Named<*>> =
      BackStackScreen(newRendering.stack.map { Named(it, "backstack") }, newRendering.onGoBack)

    val oldViewMaybe = showing

    // If existing view is compatible, just update it.
    oldViewMaybe
        ?.takeIf { it.canShowRendering(named.top) }
        ?.let {
          viewStateCache.prune(named.stack)
          it.showRendering(named.top)
          return
        }

    val newView = registry.buildView(named.top, this)
    val popped = viewStateCache.update(named.backStack, oldViewMaybe, newView)

    performTransition(oldViewMaybe, newView, popped)
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

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
  }

  override fun onSaveInstanceState(): Parcelable {
    showing?.let { viewStateCache.save(it) }
    return SavedState(super.onSaveInstanceState(), viewStateCache)
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          restored = it.viewStateCache
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
