@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composebackstack

import androidx.animation.AnimationBuilder
import androidx.animation.AnimationEndReason.TargetReached
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.ui.animation.animatedFloat
import androidx.ui.core.LayoutModifier
import androidx.ui.core.ModifierScope
import androidx.ui.core.Opacity
import androidx.ui.foundation.Box
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import com.squareup.sample.composebackstack.Direction.Backward
import com.squareup.sample.composebackstack.Direction.Forward

interface BackstackTransitions {

  @Composable
  fun drawPrevious(
    direction: Direction,
    onEnd: () -> Unit,
    drawScreen: @Composable() () -> Unit
  )

  @Composable
  fun drawNext(
    direction: Direction,
    onEnd: () -> Unit,
    drawScreen: @Composable() () -> Unit
  )

  abstract class CoordinatedTransition : BackstackTransitions {

    var animationBuilder: AnimationBuilder<Float> = TweenBuilder<Float>()
        .apply { duration = 1000 }

    @Composable
    abstract fun drawPrevious(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    )

    @Composable
    abstract fun drawNext(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    )

    @Composable
    final override fun drawPrevious(
      direction: Direction,
      onEnd: () -> Unit,
      drawScreen: @Composable() () -> Unit
    ) {
      val progress = animatedProgress(direction == Backward, onEnd)
      drawPrevious(progress, drawScreen)
    }

    @Composable
    final override fun drawNext(
      direction: Direction,
      onEnd: () -> Unit,
      drawScreen: @Composable() () -> Unit
    ) {
      val progress = animatedProgress(direction == Forward, onEnd)
      drawNext(progress, drawScreen)
    }

    @Composable
    private fun animatedProgress(
      visible: Boolean,
      onAnimationFinish: () -> Unit = {}
    ): Float {
      val animatedFloat =
        animatedFloat(if (!visible) 1f else 0f)
      onCommit(visible) {
        animatedFloat.animateTo(
            if (visible) 1f else 0f,
            anim = animationBuilder,
            onEnd = { reason, _ ->
              if (reason == TargetReached) {
                onAnimationFinish()
              }
            })
      }
      return animatedFloat.value
    }
  }

  object Slide : CoordinatedTransition() {
    @Composable
    override fun drawPrevious(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    ) {
      val offset = -1f + visibleProgress
      Box(
          modifier = PercentageLayoutOffset(offset), children = drawScreen
      )
    }

    @Composable
    override fun drawNext(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    ) {
      val offset = 1f - visibleProgress
      Box(
          modifier = PercentageLayoutOffset(offset), children = drawScreen
      )
    }

    private class PercentageLayoutOffset(private val offset: Float) : LayoutModifier {
      override fun ModifierScope.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize
      ): IntPxPosition {
        val realOffset = offset.coerceIn(-1f..1f)
        return IntPxPosition(
            x = containerSize.width * realOffset,
            y = 0.ipx
        )
      }
    }
  }

  object Crossfade : CoordinatedTransition() {
    @Composable
    override fun drawPrevious(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    ) {
      Opacity(opacity = visibleProgress, children = drawScreen)
    }

    @Composable
    override fun drawNext(
      visibleProgress: Float,
      drawScreen: @Composable() () -> Unit
    ) {
      Opacity(opacity = visibleProgress, children = drawScreen)
    }
  }
}
