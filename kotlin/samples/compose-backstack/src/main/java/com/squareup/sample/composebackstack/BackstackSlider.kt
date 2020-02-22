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
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package com.squareup.sample.composebackstack

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.core.Clip
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.unit.px

/**
 * Draws a "backstack" of screens that slide horizontally between each other.
 *
 * Effectively just a [androidx.ui.foundation.HorizontalScroller] without a drag gesture.
 */
@Composable fun <T> BackstackSlider(
  screenList: List<T>,
  @FloatRange(from = 0.0, to = 1.0) scrollPosition: Float = 1f,
  drawScreen: @Composable() (T) -> Unit
) {
  if (screenList.isEmpty()) return

  Clip(shape = RectangleShape) {
    Layout(children = { screenList.forEach { drawScreen(it) } }) { measurables, constraints ->
      val layoutWidth = constraints.maxWidth
      val layoutHeight = constraints.maxHeight
      val childConstraints = Constraints.fixed(layoutWidth, layoutHeight)
      val totalWidth = (screenList.size - 1) * layoutWidth.value
      val offset = -totalWidth * scrollPosition

      // Measure each child to fit exactly our space.
      val placeables = measurables.map { it.measure(childConstraints) }

      layout(layoutWidth, layoutHeight) {
        placeables.forEachIndexed { index, placeable ->
          val x = index * layoutWidth.value
          placeable.place(x.px + offset.px, 0.px)
        }
      }
    }
  }
}