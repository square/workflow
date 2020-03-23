@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composebackstack

import androidx.compose.Composable
import androidx.compose.invalidate
import androidx.compose.key
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.Opacity
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

private data class ScreenDrawer<T : Any>(
  val key: T,
  val children: @Composable() (@Composable() () -> Unit) -> Unit
)

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
  var activeKeys by state { backstack }
  // Wrap all items to draw in a list, so that they will all share a constant "compositional
  // position", which allows us to use the key function to preserve state.
  var activeStackDrawers by state {
    backstack.map { key ->
      ScreenDrawer(key) { children ->
        children()
      }
    }
  }
  // Null means not transitioning.
  var direction by state<Direction?> { null }
  val invalidate = invalidate
  val latch = remember {
    ResettableCountdownLatch(2) {
      println("Transition $direction finished.")
      direction = null
      reset()
      invalidate()
    }
  }

  // The stack must be drawn before the logic to determine whether to start a new transition,
  // otherwise if another transition is started immediately after the last one finishes the
  // transition composables will never be disposed, and it won't animate, it will just jump.
  Stack {
//    when (@Suppress("NAME_SHADOWING") val direction = direction) {
//      null -> drawScreen(currentTop)
//      Forward -> {
//        transitions.drawPrevious(direction, onEnd = latch::countDown) { drawScreen(currentTop) }
//        transitions.drawNext(direction, onEnd = latch::countDown) { drawScreen(newTop) }
//      }
//      Backward -> {
//        transitions.drawPrevious(direction, onEnd = latch::countDown) { drawScreen(newTop) }
//        transitions.drawNext(direction, onEnd = latch::countDown) { drawScreen(currentTop) }
//      }
//    }
    activeStackDrawers.forEach { (item, drawer) ->
      key(item) {
        drawer {
          drawScreen(item)
        }
      }
    }
  }

  // Not in the middle of a transition and we got a new backstack.
  // This will also run after a transition, to clean up old keys out of the temporary backstack.
  if (direction == null && activeKeys != backstack) {
    val oldTop = activeKeys.last()
    val newTop = backstack.last()

    if (newTop == oldTop) {
      // No transition, we're just updating the backstack.
      println(
          """
            Updating backstack without transition:
              from: $activeKeys
                to: $backstack
          """.trimIndent()
      )
      @Suppress("UNUSED_VALUE")
      activeKeys = backstack
      activeStackDrawers = backstack.map { key ->
        ScreenDrawer(key) { children ->
          children()
        }
      }
      return
    }

    // If the new top is in the old backstack, then the user has probably seen it already,
    // so this they should see this transition as going backwards.
    val localDirection = if (newTop in activeKeys) Backward else Forward
    direction = localDirection

    val newKeys = backstack.toMutableList()
    if (localDirection == Backward) {
      // We need to put the current screen on the top of the new active stack so it will animate
      // out.
      newKeys += oldTop
    } else {
      // If the current screen is not the new penultimate screen, we need to move it to that
      // position, so it animates out. This is true whether or not the current screen is in the
      // new backstack at all.
      newKeys -= newTop
      newKeys -= oldTop
      newKeys += oldTop
      newKeys += newTop
    }
    println(
        """
          Transitioning $localDirection:
            from: $activeKeys
              to: $backstack
            with: $newKeys
        """.trimIndent()
    )
    @Suppress("UNUSED_VALUE")
    activeKeys = newKeys

    val topIndex = backstack.size - 1
    val subIndex = backstack.size - 2
    activeStackDrawers = backstack.mapIndexed { index, key ->
      ScreenDrawer(key) { children ->
        when (index) {
          topIndex -> transitions.drawTop(
              localDirection,
              onEnd = latch::countDown,
              drawScreen = children
          )
          subIndex -> transitions.drawBelowTop(
              localDirection,
              onEnd = latch::countDown,
              drawScreen = children
          )
          // Still compose other children, so they retain state, but don't actually draw them.
          else -> Opacity(0f, children)
        }
      }
    }
  }
}
