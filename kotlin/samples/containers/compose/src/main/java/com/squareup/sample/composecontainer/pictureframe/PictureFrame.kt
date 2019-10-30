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
// See https://youtrack.jetbrains.com/issue/KT-31734
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composecontainer.pictureframe

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.DrawModifier
import androidx.ui.core.Text
import androidx.ui.foundation.Box
import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Radius
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.StrokeCap.square
import androidx.ui.graphics.withSaveLayer
import androidx.ui.layout.Center
import androidx.ui.layout.Padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.toRect
import androidx.ui.unit.withDensity

/**
 * Composable function that draws a fancy picture frame around its children.
 */
@Composable fun PictureFrame(
  thickness: Dp,
  colors: List<Color> = listOf(Color.Green.copy(green = .75f), Color.Red, Color.Blue),
  children: @Composable() () -> Unit
) {
  val cache = remember { PictureFrameCache() }
  Center {
    Box(
        modifier = PictureFrameModifier(thickness, colors, cache),
        children = children
    )
  }
}

@Preview(widthDp = 100, heightDp = 150)
@Composable private fun PictureFramePreview() {
  MaterialTheme {
    PictureFrame(thickness = 5.dp) {
      Surface {
        Padding(10.dp) {
          Text("Hello")
        }
      }
    }
  }
}

/**
 * This cache/modifier pair is the same technique `Border` uses.
 */
private class PictureFrameCache {
  var lastSize: PxSize? = null
  var paints: List<Paint>? = null
  var offsets: List<List<Pair<Offset, Offset>>>? = null
  var edges: List<List<Offset>>? = null
}

private class PictureFrameModifier(
  private val thickness: Dp,
  private val colors: List<Color>,
  private val cache: PictureFrameCache
) : DrawModifier {

  override fun draw(
    density: Density,
    drawContent: () -> Unit,
    canvas: Canvas,
    size: PxSize
  ) {
    // Re-calculate when the size changes.
    if (cache.lastSize != size) {
      cache.lastSize = size
      cache.offsets = null
      cache.paints = null
      cache.edges = null
    }

    val thicknessPx = withDensity(density) { thickness.toPx() }
        .value
    val paints = cache.paints ?: colors.map { color ->
      Paint().also {
        it.strokeWidth = thicknessPx
        it.strokeCap = square
        it.color = color
      }
    }.also { cache.paints = it }

    val offsets = cache.offsets ?: run {
      val parentWidth = size.width.value
      val parentHeight = size.height.value
      List(colors.size) { index: Int ->
        val indexOffset = index + 1
        listOf(
            // top left
            Offset(0f, thicknessPx * indexOffset) to Offset(thicknessPx * indexOffset, 0f),
            // top right
            Offset(parentWidth - thicknessPx * indexOffset, 0f) to Offset(
                parentWidth, thicknessPx * indexOffset
            ),
            // bottom right
            Offset(parentWidth, parentHeight - thicknessPx * indexOffset) to Offset(
                parentWidth - thicknessPx * indexOffset, parentHeight
            ),
            // bottom left
            Offset(thicknessPx * indexOffset, parentHeight) to Offset(
                0f, parentHeight - thicknessPx * indexOffset
            )
        )
      }
    }.also { cache.offsets = it }

    // Rotate all the corner points around the get the points for the edges in between them.
    val edges = cache.edges ?: run {
      offsets.last()
          .flatMap { it.toList() }
          .shifted()
          .chunked(2)
    }.also { cache.edges = it }

    val edgeRadius = withDensity(density) { thickness.toPx() }
    canvas.withSaveLayer(size.toRect(), Paint()) {
      canvas.clipRRect(RRect(size.toRect(), Radius.circular(edgeRadius.value)))
      drawContent()

      for ((index, corners) in offsets.withIndex()) {
        for ((p1, p2) in corners) {
          canvas.drawLine(p1, p2, paints[index])
        }
      }

      val innerPaint = paints.last()
      for ((p1, p2) in edges) {
        canvas.drawLine(p1, p2, innerPaint)
      }
    }
  }
}

private fun <T> List<T>.shifted(): List<T> = subList(1, size) + first()
