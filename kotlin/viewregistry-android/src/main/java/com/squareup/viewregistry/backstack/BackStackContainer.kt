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
package com.squareup.viewregistry.backstack

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.squareup.viewregistry.BackStackScreen
import com.squareup.viewregistry.BuilderBinding
import com.squareup.viewregistry.HandlesBack
import com.squareup.viewregistry.ViewBinding
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.backstack.ViewStateStack.SavedState
import com.squareup.viewregistry.takeWhileAttached
import io.reactivex.Observable

/**
 * A container view that can display a stream of [BackStackScreen] instances.
 *
 * This view is back button friendly -- it implements [HandlesBack], delegating
 * to displayed views that implement that interface themselves.
 */
class BackStackContainer(
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
    showing?.let { if (it.backStackKey == newScreen.key) return }

    val updateTools = viewStateStack.prepareToUpdate(newScreen.key)

    showing
        ?.let {
          updateTools.saveOldView(it)
          viewRegistry.getEffect(it.backStackKey, newScreen.key, updateTools.direction)
              .execute(
                  from = it,
                  to = newScreen,
                  screens = screens,
                  viewRegistry = viewRegistry,
                  container = this,
                  setUpNewView = updateTools::setUpNewView,
                  direction = updateTools.direction
              )
        }
        ?: NoEffect.execute(
            newScreen,
            screens,
            viewRegistry,
            this,
            updateTools::setUpNewView
        )
  }

  override fun onBackPressed(): Boolean {
    return showing
        ?.let { HandlesBack.Helper.onBackPressed(it) }
        ?: false
  }

  override fun onSaveInstanceState(): Parcelable {
    showing?.let { viewStateStack.save(it) }
    return SavedState(super.onSaveInstanceState(), viewStateStack)
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          restored = it.viewStateStack
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  companion object : ViewBinding<BackStackScreen<*>>
  by BuilderBinding(
      type = BackStackScreen::class.java,
      builder = { screens, viewRegistry, context, _ ->
        BackStackContainer(context).apply {
          layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
          takeScreens(screens, viewRegistry)
        }
      }
  )
}
