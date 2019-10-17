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
package com.squareup.sample.container.panel

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.FrameLayout
import com.squareup.sample.container.R
import com.squareup.workflow.ui.ModalContainer
import com.squareup.workflow.ui.ViewBinding
import kotlin.math.min

/**
 * Used by Tic Tac Workflow sample to show its [PanelContainerScreen]s.
 *
 * [ModalContainer.forContainerScreen] does most of the heavy lifting. We give
 * it a `modalDecorator` that wraps the given views in one that sizes itself
 * based on the screen size. The result looks suspiciously like the modal
 * flow container in Square PoS.
 */
object PanelContainer : ViewBinding<PanelContainerScreen<*, *>>
by ModalContainer.forContainerScreen(
    R.id.panel_container,
    modalDecorator = { panelBody ->
      PanelBodyWrapper(panelBody.context)
          .apply { addView(panelBody) }
    })

/**
 * [FrameLayout] that calculates its size based on the screen size -- to fill the screen on
 * phones, or make a square based on the shorter screen dimension on tablets. Handy
 * for showing a `Dialog` window that is set to `WRAP_CONTENT`, like those created by
 * [ModalContainer.forContainerScreen].
 */
internal class PanelBodyWrapper
@JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
  init {
    @Suppress("DEPRECATION")
    background = ColorDrawable(resources.getColor(R.color.panelBody))
  }

  /** For use only by [onMeasure]. Instantiated here to avoid allocation during measure. */
  private val displayMetrics = DisplayMetrics()

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    context.display.getMetrics(displayMetrics)
    val calculatedWidthSpec: Int
    val calculatedHeightSpec: Int

    if (context.isTablet) {
      val size = min(displayMetrics.widthPixels, displayMetrics.heightPixels)

      calculatedWidthSpec = makeMeasureSpec(size, EXACTLY)
      calculatedHeightSpec = makeMeasureSpec(size, EXACTLY)
    } else {
      calculatedWidthSpec = makeMeasureSpec(displayMetrics.widthPixels, EXACTLY)
      calculatedHeightSpec = makeMeasureSpec(displayMetrics.heightPixels, EXACTLY)
    }

    super.onMeasure(calculatedWidthSpec, calculatedHeightSpec)
  }
}
