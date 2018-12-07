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
import android.widget.FrameLayout
import com.squareup.viewbuilder.ViewStack.ReplaceResult.PUSHED
import com.squareup.viewbuilder.ViewStack.SavedState

class ViewStackFrameLayout(
  context: Context,
  attributeSet: AttributeSet
) : FrameLayout(context, attributeSet), HandlesBack {
  private var restored: ViewStack? = null
  private val viewStack by lazy { restored ?: ViewStack() }

  val showing: View? get() = if (childCount > 0) getChildAt(0) else null

  fun show(newView: View) {
    showing?.let {
      if (it.betterKey == newView.betterKey) return

      val pushedOrPopped = viewStack.replace(it, newView)
      val slideEdge = if (pushedOrPopped == PUSHED) Gravity.END else Gravity.START

      TransitionManager.beginDelayedTransition(this, Slide(slideEdge))
      removeAllViews()
    }

    addView(newView)
  }

  override fun onSaveInstanceState(): Parcelable {
    return SavedState(super.onSaveInstanceState(), viewStack)
  }

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          restored = it.viewStack
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }
}
