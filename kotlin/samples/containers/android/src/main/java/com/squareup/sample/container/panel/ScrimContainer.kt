/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.container.panel

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.squareup.sample.container.R
import com.squareup.workflow.ui.BuilderBinding
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.WorkflowViewStub
import com.squareup.workflow.ui.bindShowRendering

/**
 * A view that renders only its first child, behind a smoke scrim if
 * [isDimmed] is true (tablets only). Other children are ignored.
 *
 * Able to [render][com.squareup.workflow.ui.showRendering] [ScrimContainerScreen].
 */
class ScrimContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0
) : ViewGroup(context, attributeSet, defStyle, defStyleRes) {
  private val scrim = object : View(context, attributeSet, defStyle, defStyleRes) {
    init {
      @Suppress("DEPRECATION")
      setBackgroundColor(resources.getColor(R.color.scrim))
    }
  }

  private val child: View
    get() = getChildAt(0)
        ?: error("Child must be set immediately upon creation.")

  var isDimmed: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      if (!isAttachedToWindow) updateImmediate() else updateAnimated()
    }

  override fun onAttachedToWindow() {
    updateImmediate()
    super.onAttachedToWindow()
  }

  override fun addView(child: View?) {
    if (scrim.parent != null) removeView(scrim)
    super.addView(child)
    super.addView(scrim)
  }

  override fun onLayout(
    changed: Boolean,
    l: Int,
    t: Int,
    r: Int,
    b: Int
  ) {
    child.layout(0, 0, measuredWidth, measuredHeight)
    scrim.layout(0, 0, measuredWidth, measuredHeight)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    child.measure(widthMeasureSpec, heightMeasureSpec)
    scrim.measure(widthMeasureSpec, heightMeasureSpec)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  private fun updateImmediate() {
    if (isDimmed) scrim.alpha = 1f else scrim.alpha = 0f
  }

  private fun updateAnimated() {
    if (isDimmed) {
      ValueAnimator.ofFloat(0f, 1f)
    } else {
      ValueAnimator.ofFloat(1f, 0f)
    }.apply {
      duration = resources.getInteger(android.R.integer.config_shortAnimTime)
          .toLong()
      addUpdateListener { animation -> scrim.alpha = animation.animatedValue as Float }
      start()
    }
  }

  companion object : ViewFactory<ScrimContainerScreen<*>> by BuilderBinding(
      type = ScrimContainerScreen::class,
      viewConstructor = { initialRendering, initialViewEnvironment, contextForNewView, _ ->
        val stub = WorkflowViewStub(contextForNewView)

        ScrimContainer(contextForNewView)
            .apply {
              layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
              addView(stub)

              bindShowRendering(
                  initialRendering, initialViewEnvironment
              ) { rendering, environment ->
                stub.update(rendering.wrapped, environment)
                isDimmed = rendering.dimmed
              }
            }
      }
  )
}
