@file:Suppress("UNUSED_VALUE", "RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.sample.composecontainer.pictures

import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.Transition
import androidx.ui.core.Text
import androidx.ui.tooling.preview.Preview
import com.squareup.sample.composecontainer.pictures.SlideState.Idle
import com.squareup.sample.composecontainer.pictures.SlideState.SlidingIn

/**
 * TODO write documentation
 */
private enum class SlideState {
  Idle,
  SlidingIn
}

private val amountKey = FloatPropKey()
private val slideTransition = transitionDefinition {
  state(Idle) { this[amountKey] = 0f }
  state(SlidingIn) { this[amountKey] = 1f }

  // Don't bounce the animation back when resetting to Idle after the transition.
  snapTransition(SlidingIn to Idle)
}

private class SlideValues(
  var lastKey: Any?,
  var incomingKey: Any?,
  var outgoingKey: Any?
)

@Suppress("UNCHECKED_CAST")
@Composable fun <T> SlideIn(
  key: T,
  children: @Composable() (key: T) -> Unit
) {
  var slideState: SlideState by state { Idle }
  val values = remember { SlideValues(key, key, key) }

  if (slideState == Idle && values.lastKey != key) {
    slideState = SlidingIn
    values.incomingKey = key
    values.outgoingKey = values.lastKey
  }
  values.lastKey = key

  Transition(
      definition = slideTransition,
      toState = slideState,
      onStateChangeFinished = {
        slideState = Idle
        values.outgoingKey = values.incomingKey
      }
  ) { transitionState ->
    HorizontalShift(
        amount = transitionState[amountKey],
        left = { children(values.outgoingKey as T) },
        right = { children(values.incomingKey as T) }
    )
  }
}

@Preview
@Composable private fun SlideInPreview() {
  SlideIn(key = "key") { key ->
    Text(key)
  }
}
