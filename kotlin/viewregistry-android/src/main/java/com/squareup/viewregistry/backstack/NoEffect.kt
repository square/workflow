package com.squareup.viewregistry.backstack

import android.view.View
import android.view.ViewGroup
import com.squareup.viewregistry.BackStackScreen
import com.squareup.viewregistry.BackStackScreen.Key
import com.squareup.viewregistry.ViewRegistry
import com.squareup.viewregistry.backstack.ViewStateStack.Direction
import io.reactivex.Observable
import kotlin.reflect.KClass

/**
 * Empties the container and adds the view to it with no animation.
 * This is the default effect returned by [ViewRegistry.effects]
 * if no other is found.
 */
class NoEffect(
  private val from: Key<*>?,
  private val to: Key<*>?,
  private val direction: Direction?
) : BackStackEffect {
  constructor(
    from: KClass<*>? = null,
    to: KClass<*>? = null,
    direction: Direction? = null
  ) : this(from?.let { Key(it) }, to?.let { Key(it) }, direction)

  override fun matches(
    from: Key<*>,
    to: Key<*>,
    direction: Direction
  ): Boolean {
    return (this.from == null || this.from == from) &&
        (this.to == null || this.to == to) &&
        (this.direction == null || this.direction == direction)
  }

  override fun execute(
    from: View,
    to: BackStackScreen<*>,
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry,
    container: ViewGroup,
    setUpNewView: (View) -> Unit,
    direction: Direction
  ) {
    execute(to, screens, viewRegistry, container, setUpNewView)
  }

  companion object : BackStackEffect by NoEffect() {

    /**
     * Empties [container] and makes [to] its only child.
     */
    fun execute(
      to: BackStackScreen<*>,
      screens: Observable<out BackStackScreen<*>>,
      viewRegistry: ViewRegistry,
      container: ViewGroup,
      setUpNewView: (View) -> Unit
    ) {
      container.removeAllViews()
      val newView = to.viewForWrappedScreen(screens, viewRegistry, container)
          .apply { setUpNewView(this) }
      container.addView(newView)
    }
  }
}
