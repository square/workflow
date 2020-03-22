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
package com.squareup.sample.composebackstack

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.foundation.Box
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.material.Button
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp

@Preview
@Composable fun AppScreenPreview() {
  AppScreen(name = "preview")
}

val markerRadius = 24.dp
@Composable fun AppScreen(name: String) {
  Surface {
    Stack(modifier = LayoutSize.Fill) {
      Box(
          shape = CircleShape,
          modifier = LayoutGravity.TopStart +
              LayoutSize(markerRadius * 2) +
              LayoutPadding(start = -markerRadius, top = -markerRadius),
          backgroundColor = Color.Red
      ) {}
      Box(
          shape = CircleShape,
          modifier = LayoutGravity.TopEnd +
              LayoutSize(markerRadius * 2) +
              LayoutPadding(end = -markerRadius, top = -markerRadius),
          backgroundColor = Color.Red
      ) {}
      Box(
          shape = CircleShape,
          modifier = LayoutGravity.BottomStart +
              LayoutSize(markerRadius * 2) +
              LayoutPadding(start = -markerRadius, bottom = -markerRadius),
          backgroundColor = Color.Red
      ) {}
      Box(
          shape = CircleShape,
          modifier = LayoutGravity.BottomEnd +
              LayoutSize(markerRadius * 2) +
              LayoutPadding(end = -markerRadius, bottom = -markerRadius),
          backgroundColor = Color.Red
      ) {}

      var counter by state { 0 }
      Column(modifier = LayoutGravity.Center) {
        Text(name, modifier = LayoutGravity.Center)
        Button(onClick = { counter++ }) {
          Text("Counter: $counter")
        }
      }
    }
  }
}
