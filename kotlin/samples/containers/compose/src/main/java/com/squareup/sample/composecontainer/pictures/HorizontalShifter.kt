@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composecontainer.pictures

import androidx.compose.Composable
import androidx.ui.core.LayoutModifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.DensityScope
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Preview @Composable fun HorizontalShiftPreview0() {
  HorizontalShiftPreviewContent(amount = 0f)
}

@Preview @Composable fun HorizontalShiftPreview2() {
  HorizontalShiftPreviewContent(amount = .2f)
}

@Preview @Composable fun HorizontalShiftPreview5() {
  HorizontalShiftPreviewContent(amount = .5f)
}

@Preview @Composable fun HorizontalShiftPreview7() {
  HorizontalShiftPreviewContent(amount = .7f)
}

@Preview @Composable fun HorizontalShiftPreview10() {
  HorizontalShiftPreviewContent(amount = 1f)
}

@Composable fun HorizontalShiftPreviewContent(amount: Float) {
  @Composable fun Child(
    color: Color,
    width: Dp,
    height: Dp = 10.dp
  ) {
    Box(modifier = LayoutSize(width, height)) {
      DrawShape(shape = RectangleShape, color = color)
    }
  }

  Surface {
    HorizontalShift(
        amount = amount,
        left = { Child(Color.Red, width = 50.dp) },
        right = { Child(Color.Green, width = 50.dp) }
    )
  }
}

@Composable internal fun HorizontalShift(
  amount: Float,
  left: @Composable() () -> Unit,
  right: @Composable() () -> Unit
) {
  require(amount in 0f..1f)
  Stack {
    Box(modifier = OffsetWidth(-amount)) { left() }
    Box(modifier = OffsetWidth(1 - amount)) { right() }
  }
}

private data class OffsetWidth(val amount: Float) : LayoutModifier {
  init {
    require(amount in -1f..1f)
  }

  override fun DensityScope.modifyPosition(
    childSize: IntPxSize,
    containerSize: IntPxSize
  ): IntPxPosition = IntPxPosition(childSize.width * amount, 0.ipx)
}
