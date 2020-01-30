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
package com.squareup.sample.composecontainer.pictures

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.onCommit
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Draw
import androidx.ui.core.WithConstraints
import androidx.ui.core.ambientDensity
import androidx.ui.foundation.Box
import androidx.ui.graphics.Canvas
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.withDensity
import coil.Coil
import coil.api.load
import coil.size.Precision.EXACT
import coil.size.Scale.FIT
import com.squareup.sample.composecontainer.pictures.PicturesWorkflow.Rendering
import com.squareup.workflow.ui.compose.bindCompose
import com.squareup.workflow.ui.compose.tooling.preview

val PicturesBinding = bindCompose<Rendering> { rendering, _ ->
  WithConstraints { constraints ->
    val image = image(
        url = rendering.pictureUrl,
        width = constraints.maxWidth,
        height = constraints.maxHeight
    )

    if (image == null) {
      Box(modifier = LayoutPadding(200.dp), gravity = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      withDensity(ambientDensity()) {
        Box(modifier = LayoutSize(image.width.toDp(), image.height.toDp())) {
          Draw { canvas: Canvas, _: PxSize ->
            canvas.nativeCanvas.drawBitmap(image, 0f, 0f, null)
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun PicturesBindingPreview() {
  MaterialTheme {
    Surface {
      PicturesBinding.preview(
          Rendering(
              pictureUrl = "todo",
              pictureDescription = "The Picture",
              onTap = {}
          )
      )
    }
  }
}

@Composable private fun image(
  url: String,
  width: IntPx,
  height: IntPx
): Bitmap? {
  val image = state<Bitmap?> { null }
  val context = ambient(ContextAmbient)

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
