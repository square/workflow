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

package com.squareup.sample.composecontainer.pictures

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.ContextAmbient
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Draw
import androidx.ui.core.Text
import androidx.ui.core.WithConstraints
import androidx.ui.foundation.Box
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.Center
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize
import coil.Coil
import coil.api.load
import coil.size.Precision.EXACT
import coil.size.Scale.FIT

typealias DrawImage = @Composable() () -> Unit

val CoilPreviewUrl = "http://example.com"

/**
 * Loads an image from the network using [Coil] and draws it into the composition.
 *
 * @param drawLoading Composable to draw while the image is being loaded.
 * @param drawLoaded Composable to draw when the image has loaded successfully. This function
 * receives another composable function that it should call to actually draw the image.
 */
@Composable fun CoilImage(
  url: String = CoilPreviewUrl,
  drawLoading: @Composable() () -> Unit = {},
  drawLoaded: @Composable() (DrawImage) -> Unit = { it() }
) {
  WithConstraints { constraints ->
    if (url == CoilPreviewUrl) {
      CoilPreviewContent()
      return@WithConstraints
    }

    val image = image(
        url = url,
        width = constraints.maxWidth,
        height = constraints.maxHeight
    )

    if (image == null) {
      drawLoading()
    } else {
      drawLoaded {
        with(DensityAmbient.current) {
          Box(modifier = LayoutSize(image.width.toDp(), image.height.toDp())) {
            Draw { canvas: Canvas, _: PxSize ->
              canvas.nativeCanvas.drawBitmap(image, 0f, 0f, null)
            }
          }
        }
      }
    }
  }
}

@Preview(widthDp = 100, heightDp = 75)
@Composable private fun CoilImagePreview() {
  CoilImage()
}

@Composable private fun CoilPreviewContent() {
  val crossPaint = remember {
    Paint().also {
      it.color = Color.Red
    }
  }
  Stack {
    DrawShape(shape = RectangleShape, color = Color.White)
    Draw { canvas, parentSize ->
      canvas.drawLine(
          Offset.zero,
          Offset(parentSize.width.value, parentSize.height.value),
          crossPaint
      )
      canvas.drawLine(
          Offset(0f, parentSize.height.value),
          Offset(parentSize.width.value, 0f),
          crossPaint
      )
    }
    Center {
      Text("{image}")
    }
  }
}

@Composable private fun image(
  url: String,
  width: IntPx,
  height: IntPx
): Bitmap? {
  val image = state<Bitmap?> { null }
  val context = ContextAmbient.current

  onCommit(url, width, height, context) {
    val requestDisposable = Coil.load(context, url) {
      size(width.value, height.value)
      precision(EXACT)
      scale(FIT)
      target(
          onSuccess = { image.value = (it as? BitmapDrawable)?.bitmap },
          onError = { image.value = null }
      )
    }

    onDispose { requestDisposable.dispose() }
  }

  return image.value
}
