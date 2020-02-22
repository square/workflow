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
@file:Suppress(
    "RemoveEmptyParenthesesFromAnnotationEntry",
    "RemoveForLoopIndices",
    "UNUSED_VARIABLE"
)

package com.squareup.sample.composebackstack

import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.foundation.Box
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutSize
import androidx.ui.material.ListItem
import androidx.ui.material.Slider
import androidx.ui.material.SliderPosition
import androidx.ui.tooling.preview.Preview
import com.squareup.sample.composebackstack.BackstackImpl.Fader

enum class BackstackImpl {
  Fader,
  Slider
}

private val screens = listOf(
    "Main Screen\n\n",
    "\nSecond Screen\n",
    "\n\nThird Screen"
)

@Composable fun App() {
  var selectedBackstackImpl by state { Fader }

  val scrollPosition = remember { SliderPosition(initial = 1f) }
  Column(modifier = LayoutSize.Fill) {
    Spinner(
        items = BackstackImpl.values().asList(),
        selectedIndex = selectedBackstackImpl.ordinal,
        onSelected = { selectedBackstackImpl = BackstackImpl.values()[it] }
    ) {
      ListItem(text = it.toString())
    }

    Box(modifier = LayoutFlexible(1f)) {
      when (selectedBackstackImpl) {
        Fader -> {
          BackstackFader(
              screenList = screens,
              scrollPosition = scrollPosition.value
          ) {
            AppScreen(name = it)
          }
        }
        BackstackImpl.Slider -> {
          BackstackSlider(
              screenList = screens,
              scrollPosition = scrollPosition.value
          ) {
            AppScreen(name = it)
          }
        }
      }
    }

    Slider(position = scrollPosition)
  }
}

@Preview
@Composable fun AppPreview() {
  App()
}
