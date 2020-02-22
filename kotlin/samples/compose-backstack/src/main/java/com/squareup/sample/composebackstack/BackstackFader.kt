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
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composebackstack

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.core.Opacity
import androidx.ui.layout.Stack

/**
 * Draws a "backstack" of screens that fade between each other.
 *
 * @param scrollPosition The position in the backstack to display, where 0 means show only the
 * first screen and 1 means show only the last screen. In a backstack of size 2, 0.5 means show the
 * second screen 50% faded over the first one.
 */
@Composable fun <T> BackstackFader(
  screenList: List<T>,
  @FloatRange(from = 0.0, to = 1.0) scrollPosition: Float = 1f,
  drawScreen: @Composable() (T) -> Unit
) {
  if (screenList.isEmpty()) return

  // Always draw the first screen, at full opacity.
  drawScreen(screenList.first())

  // Draw the rest of the screens.
  val restOfScreens = screenList.drop(1)
  val stepSize = 1f / restOfScreens.size
  Stack {
    for ((i, screen) in restOfScreens.withIndex()) {
      val start = stepSize * i
      val end = start + stepSize
      // If we're going to be drawn with 0 opacity, don't compose at all. Loses view state, which
      // is reasonable behavior for a backstack, and also stops blocking input events to the next
      // screen in the stack.
      if (scrollPosition <= start) continue
      val opacity = if (scrollPosition >= end) 1f else (scrollPosition - start) / stepSize
      Opacity(opacity) {
        drawScreen(screen)
      }
    }
  }
}
