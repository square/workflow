package com.squareup.viewregistry

import android.support.transition.Fade
import android.support.transition.Slide
import android.support.transition.TransitionManager
import android.support.transition.TransitionSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.squareup.viewregistry.ViewStateStack.Direction.PUSH
import com.squareup.viewregistry.ViewStateStack.UpdateTools
import io.reactivex.Observable

/**
 * Performs a pretty lame push or pop animation, just to prove that we can.
 */
object PushPopEffect : BackStackEffect {
  /**
   * Does not rely on [from] to have a [View.backStackKey].
   */
  override fun execute(
    from: View,
    to: BackStackScreen<*>,
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry,
    container: ViewGroup,
    tools: UpdateTools
  ) {
    val newScene = to
        .buildWrappedScene(screens, viewRegistry, container) { scene ->
          scene.viewOrNull()
              ?.let { tools.setUpNewView(it) }
        }

    tools.saveOldView(from)

    val outEdge = if (tools.direction == PUSH) Gravity.START else Gravity.END
    val inEdge = if (tools.direction == PUSH) Gravity.END else Gravity.START

    val outSet = TransitionSet()
        .addTransition(Slide(outEdge).addTarget(from))
        .addTransition(Fade(Fade.OUT))

    val fullSet = TransitionSet()
        .addTransition(outSet)
        .addTransition(Slide(inEdge).excludeTarget(from, true))

    TransitionManager.go(newScene, fullSet)
  }
}
