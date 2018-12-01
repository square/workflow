package com.squareup.viewbuilder

import android.content.Context
import android.os.Parcelable
import android.transition.Slide
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.squareup.viewbuilder.ViewBuilder.Registry
import com.squareup.viewbuilder.ViewStack.ReplaceResult.PUSHED
import com.squareup.viewbuilder.ViewStack.SavedState
import io.reactivex.Observable
import kotlin.reflect.jvm.jvmName

class ViewStackFrameLayout(
  context: Context,
  attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {
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

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as? SavedState)
        ?.let {
          restored = it.viewStack
          super.onRestoreInstanceState(state.superState)
        }
        ?: super.onRestoreInstanceState(state)
  }

  companion object : ViewBuilder<ViewStackScreen<*>> {
    override val type = ViewStackScreen::class.jvmName

    override fun buildView(
      screens: Observable<out ViewStackScreen<*>>,
      builders: Registry,
      contextForNewView: Context,
      container: ViewGroup?
    ): View {
      TODO("https://github.com/square/workflow/issues/21")
    }
  }
}
