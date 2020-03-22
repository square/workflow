@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composebackstack

import androidx.animation.AnimationEndReason.TargetReached
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.animation.animatedFloat
import androidx.ui.core.Opacity
import com.squareup.sample.composebackstack.Direction.Backward
import com.squareup.sample.composebackstack.Direction.Forward

enum class Direction {
  Forward,
  Backward
}

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

  object Crossfade : BackstackTransitions {
    @Composable
    override fun drawPrevious(
      direction: Direction,
      onEnd: () -> Unit,
      drawScreen: @Composable() () -> Unit
    ) {
      Opacity(
          opacity = animatedOpacity(
              visible = direction == Backward,
              onAnimationFinish = onEnd
          ),
          children = drawScreen
      )
    }

    @Composable
    override fun drawNext(
      direction: Direction,
      onEnd: () -> Unit,
      drawScreen: @Composable() () -> Unit
    ) {
      Opacity(
          opacity = animatedOpacity(
              visible = direction == Forward,
              onAnimationFinish = onEnd
          ),
          children = drawScreen
      )
    }

    @Composable
    private fun animatedOpacity(
      visible: Boolean,
      onAnimationFinish: () -> Unit = {}
    ): Float {
      val animatedFloat = animatedFloat(if (!visible) 1f else 0f)
      onCommit(visible) {
        animatedFloat.animateTo(
            if (visible) 1f else 0f,
            anim = TweenBuilder<Float>().apply { duration = 1000 },
            onEnd = { reason, _ ->
              if (reason == TargetReached) {
                onAnimationFinish()
              }
            })
      }
      return animatedFloat.value
    }
  }
}

private class BackstackState<T : Any>(
  val transitions: BackstackTransitions
) {
  var direction: Direction? = null
    private set


  var wrappedStack: List<WrappedScreen<T>> = emptyList()
    private set

  fun setActiveStack(
    newStack: List<T>,
    direction: Direction? = null,
    onEnd: () -> Unit = {}
  ) {
    this.direction = direction
    activeStack = newStack

    // Simple countdown to fire the finished callback only when both transitions are finished.
    var transitionsFinished = 0
    fun onTransitionFinished() {
      transitionsFinished++
      if (transitionsFinished == 2) {
        // Clear direction and create new wrapped stack to hide all but the top screen.
        setActiveStack(newStack)
        onEnd()
      }
    }

    wrappedStack = newStack.mapIndexed { index, item ->
      when (index) {
        newStack.size - 1 -> WrappedScreen(item) { drawScreen ->
          direction?.let {
            transitions.drawNext(it, ::onTransitionFinished, drawScreen)
          } ?: run {
            // We're not animating, just draw the screen.
            drawScreen()
          }
        }

        newStack.size - 2 -> WrappedScreen(item) { drawScreen ->
          direction?.let {
            transitions.drawPrevious(it, ::onTransitionFinished, drawScreen)
          } ?: run {
            // We're not animating, just hide the screen.
            HideScreen(drawScreen)
          }
        }

        else -> WrappedScreen(item) { drawScreen ->
          HideScreen(drawScreen)
        }
      }
    }
  }

  var activeStack: List<T> = emptyList()
    private set
}

private class WrappedScreen<T>(
  val key: T,
  val draw: @Composable() (screen: @Composable() () -> Unit) -> Unit
)

/**
 * TODO write documentation
 */
@Composable
fun <T : Any> Backstack(
  backstack: List<T>,
  transitions: BackstackTransitions = BackstackTransitions.Crossfade,
  drawScreen: @Composable() (T) -> Unit
) {
  require(backstack.isNotEmpty()) { "Backstack must contain at least 1 screen." }
  val state = remember {
    BackstackState<T>(transitions)
        .also { it.setActiveStack(backstack) }
  }

  Recompose { recompose ->
    if (state.direction == null && backstack != state.activeStack) {
      // We're not transitioning and got a new backstack, kick off a transition.
      val direction = if (backstack.size < state.activeStack.size) Backward else Forward
      println(
          """
            Transitioning $direction:
              from: ${state.activeStack.joinToString()}
                to: ${backstack.joinToString()}
          """.trimIndent()
      )
      var temporaryBackstack = backstack
      if (direction == Backward) {
        // Push the current screen onto the backstack to animate out of.
        temporaryBackstack = backstack + state.activeStack.last()
      }
      state.setActiveStack(temporaryBackstack, direction, recompose)
      recompose()
    }

    state.wrappedStack.forEach {
//      key(it.key) {
      it.draw {
        drawScreen(it.key)
      }
//      }
    }
  }
}

@Composable
private fun HideScreen(child: @Composable() () -> Unit) {
  Opacity(0f) {
    child()
  }
}
