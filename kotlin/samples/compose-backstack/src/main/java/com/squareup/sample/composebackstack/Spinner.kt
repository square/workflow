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

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Dialog
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowDropDown
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.unit.dp

/**
 * Rough implementation of the Android Spinner widget.
 */
@Composable fun <T> Spinner(
  items: List<T>,
  selectedIndex: Int = 0,
  onSelected: (index: Int) -> Unit,
  drawItem: @Composable() (T) -> Unit
) {
  if (items.isEmpty()) return

  var isOpen by state { false }

  // Always draw the selected item.
  Container {
    Ripple(bounded = true) {
      Clickable(onClick = { isOpen = !isOpen }) {
        Row {
          Box(modifier = LayoutFlexible(1f)) {
            drawItem(items[selectedIndex])
          }
          Box(modifier = LayoutWidth(48.dp) + LayoutAspectRatio(1f) + LayoutGravity.Center) {
            DrawVector(vectorImage = Icons.Default.ArrowDropDown)
          }
        }
      }
    }

    if (isOpen) {
      // TODO use DropdownPopup.
      Dialog(onCloseRequest = { isOpen = false }) {
        Surface(elevation = 1.dp) {
          Column {
            for ((i, item) in items.withIndex()) {
              Ripple(bounded = true) {
                Clickable(onClick = {
                  isOpen = false
                  if (i != selectedIndex) {
                    onSelected(i)
                  }
                }) {
                  drawItem(item)
                }
              }
            }
          }
        }
      }
    }
  }
}
