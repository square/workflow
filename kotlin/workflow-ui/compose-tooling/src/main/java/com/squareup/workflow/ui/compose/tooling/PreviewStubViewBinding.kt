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
package com.squareup.workflow.ui.compose.tooling

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Draw
import androidx.ui.core.Opacity
import androidx.ui.core.Text
import androidx.ui.core.WithDensity
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Shadow
import androidx.ui.graphics.withSave
import androidx.ui.layout.Center
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.px
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.compose.bindCompose

/**
 * TODO kdoc
 */
internal val PreviewStubViewBinding: ViewBinding<Any> = bindCompose { rendering, _ ->
  StubBackground()
  Center {
    Text(
        text = rendering.toString(),
        style = TextStyle(
            textAlign = TextAlign.Center,
            color = Color.White,
            shadow = Shadow(blurRadius = 10.px, color = Color.Black)
        )
    )
  }
}

@Preview(widthDp = 100, heightDp = 200)
@Composable private fun PreviewStubViewBinding() {
  PreviewStubViewBinding.preview(rendering = "preview")
}

@Composable private fun StubBackground() {
  Opacity(opacity = .7f) {
    DrawCrossHatch(
        color = Color.Red,
        strokeWidth = 2.dp,
        spaceWidth = 5.dp,
        angle = 45f
    )
  }
}

@Composable private fun DrawCrossHatch(
  color: Color,
  strokeWidth: Dp,
  spaceWidth: Dp,
  angle: Float
) {
  DrawHatch(color, strokeWidth, spaceWidth, angle)
  DrawHatch(color, strokeWidth, spaceWidth, angle + 90)
}

@Composable private fun DrawHatch(
  color: Color,
  strokeWidth: Dp,
  spaceWidth: Dp,
  angle: Float
) {
  WithDensity {
    val strokeWidthPx = strokeWidth.toPx()
        .value
    val paint = remember {
      Paint().also {
        it.color = color.scaleColors(.5f)
        it.strokeWidth = strokeWidthPx
      }
    }

    Draw { canvas, parentSize ->
      canvas.withSave {
        val halfWidth = parentSize.width.value / 2
        val halfHeight = parentSize.height.value / 2
        canvas.translate(halfWidth, halfHeight)
        canvas.rotate(angle)
        canvas.translate(-halfWidth, -halfHeight)

        // Draw outside our bounds to fill the space even when rotated.
        val left = -parentSize.width.value
        val right = parentSize.width.value * 2
        val top = -parentSize.height.value
        val bottom = parentSize.height.value * 2

        var y = top + strokeWidthPx * 2f
        while (y < bottom) {
          canvas.drawLine(
              Offset(left, y),
              Offset(right, y),
              paint
          )
          y += spaceWidth.toPx().value * 2
        }
      }
    }
  }
}

private fun Color.scaleColors(factor: Float) =
  copy(red = red * factor, green = green * factor, blue = blue * factor)
