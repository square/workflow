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
package com.squareup.viewbuilder

import android.content.Context
import android.os.Parcelable
import android.transition.Slide
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.squareup.viewbuilder.ViewStateStack.Direction.PUSH
import io.reactivex.Observable

/**
 * A container view that can display a stream of [BackStackScreen] instances.
 * This view is back button friendly -- it implements [HandlesBack], delegating
 * to displayed views that implement that interface themselves.
 */
class BackStackFrameLayout(
  context: Context,
  attributeSet: AttributeSet?
) : FrameLayout(context, attributeSet), HandlesBack {
  constructor(context: Context) : this(context, null)

  private var restored: ViewStateStack? = null
  private val viewStateStack by lazy { restored ?: ViewStateStack() }

  private val showing: View? get() = if (childCount > 0) getChildAt(0) else null

  fun takeScreens(
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry
  ) {
    takeWhileAttached(screens.distinctUntilChanged { a, b -> a.key == b.key }) {
      show(it, screens, viewRegistry)
    }
  }

  private fun show(
    newScreen: BackStackScreen<*>,
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry
  ) {
    val updateTools = viewStateStack.prepareToUpdate(newScreen.key)
    var slideEdge: Int? = null

    showing?.let {
      if (it.backStackKey == newScreen.key) return

      updateTools.saveOldView(it)
      slideEdge = if (updateTools.direction == PUSH) Gravity.END else Gravity.START
    }

    val newScene = newScreen
        .buildWrappedScene(screens, viewRegistry, this) { scene ->
          scene.viewOrNull()
              ?.let { updateTools.setUpNewView(it) }
        }
    slideEdge
        ?.let { TransitionManager.go(newScene, Slide(it)) }
        ?: TransitionManager.go(newScene)
  }

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
  }

  override fun onSaveInstanceState(): Parcelable {
    showing?.let { viewStateStack.save(it) }
    return ViewStateStack.SavedState(super.onSaveInstanceState(), viewStateStack)
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? ViewStateStack.SavedState)
        ?.let {
          restored = it.viewStateStack
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  companion object : ViewBinding<BackStackScreen<*>>
  by BuilderBinding(
      type = BackStackScreen::class.java,
      builder = { screens, builders, context, _ ->
        BackStackFrameLayout(context).apply {
          layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
          takeScreens(screens, builders)
        }
      }
  )
}
