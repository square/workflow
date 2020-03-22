@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composebackstack

import androidx.compose.Composable
import androidx.compose.invalidate
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.layout.Stack
import com.squareup.sample.composebackstack.Direction.Backward
import com.squareup.sample.composebackstack.Direction.Forward

enum class Direction {
  Forward,
  Backward
}

private class ResettableCountdownLatch(
  private val initialCount: Int,
  private val onZero: ResettableCountdownLatch.() -> Unit
) {
  private var count = initialCount

  fun countDown() {
    count--
    if (count == 0) onZero()
  }

  fun reset() {
    count = initialCount
  }
}

/**
 * TODO write documentation
 */
@Composable
fun <T : Any> Backstack(
  backstack: List<T>,
  transitions: BackstackTransitions = BackstackTransitions.Slide,
  drawScreen: @Composable() (T) -> Unit
) {
  require(backstack.isNotEmpty()) { "Backstack must contain at least 1 screen." }
  var currentTop by state { backstack.last() }
  var activeStack by state { backstack }
  // Null means not transitioning.
  var direction by state<Direction?> { null }
  var newTop by state { currentTop }
  val invalidate = invalidate
  val latch = remember {
    ResettableCountdownLatch(2) {
      println("Transition $direction finished.")
      currentTop = newTop
      direction = null
      reset()
      invalidate()
    }
  }

  // The stack must be drawn before the logic to determine whether to start a new transition,
  // otherwise if another transition is started immediately after the last one finishes the
  // transition composables will never be disposed, and it won't animate, it will just jump.
  Stack {
    when (@Suppress("NAME_SHADOWING") val direction = direction) {
      null -> drawScreen(currentTop)
      Forward -> {
        transitions.drawPrevious(direction, onEnd = latch::countDown) { drawScreen(currentTop) }
        transitions.drawNext(direction, onEnd = latch::countDown) { drawScreen(newTop) }
      }
      Backward -> {
        transitions.drawPrevious(direction, onEnd = latch::countDown) { drawScreen(newTop) }
        transitions.drawNext(direction, onEnd = latch::countDown) { drawScreen(currentTop) }
      }
    }
  }

  if (direction == null && currentTop != backstack.last()) {
    // Not in the middle of a transition and we got a new screen.
    newTop = backstack.last()
    direction = if (backstack.last() in activeStack) Backward else Forward
    println(
        """
          Transitioning $direction:
            from: $currentTop
              to: $newTop
        """.trimIndent()
    )
    activeStack = backstack
  }
}
