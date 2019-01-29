package com.squareup.viewregistry

import android.support.transition.Scene
import android.view.View
import android.view.ViewGroup
import com.squareup.viewregistry.ViewStateStack.Direction
import io.reactivex.Observable
import kotlin.reflect.jvm.jvmName

/**
 * An object that can change the contents of a container view (a [ViewGroup]) from the view
 * from one [BackStackScreen] to another.
 *
 * Override the default implementation of [matches] to restrict an effect to transitions
 * between specific types of views. Implement [execute] to actually perform the work.
 *
 * Effects are assembled by [ViewRegistry] instances, along with [ViewBinding]s.
 * When a container is ready to perform a transition, it can call [ViewRegistry.getEffect]
 * to retrieve the appropriate effect to run.
 */
interface BackStackEffect {
  /**
   * Returns true of this effect can be applied to the given keys and direction.
   * The default implementation always returns true, indicating that this effect
   * can handle all transitions.
   */
  fun matches(
    from: BackStackScreen.Key<*>,
    to: BackStackScreen.Key<*>,
    direction: ViewStateStack.Direction
  ): Boolean = true

  /**
   * Perform the transition. Remember that:
   *
   *  - You can use [View.backStackKey] to extract the key associated with [from].
   *
   *  - You can use [buildWrappedView] or [buildWrappedScene] to give the new view
   *    its expected stream of properly typed screen objects.
   *
   *  - You must call [setUpNewView] on the incoming view before it is attached
   *    to the window.
   */
  fun execute(
    from: View,
    to: BackStackScreen<*>,
    screens: Observable<out BackStackScreen<*>>,
    viewRegistry: ViewRegistry,
    container: ViewGroup,
    setUpNewView: (View) -> Unit,
    direction: Direction
  )
}

/**
 * Fishes in [viewRegistry] for the [ViewBinding] for type [T] and
 * uses it to instantiate a [View] to display any matching items
 * received via [screens].
 */
fun <T : Any> BackStackScreen<T>.buildWrappedView(
  screens: Observable<out BackStackScreen<*>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup
): View {
  val myScreens: Observable<out T> = screens.matching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(key.type.jvmName)
  return binding.buildView(myScreens, viewRegistry, container)
}

/**
 * Fishes in [viewRegistry] for the [ViewBinding] for type [T] and
 * uses it to instantiate a [Scene] to display any matching items
 * received via [screens].
 */
fun <T : Any> BackStackScreen<T>.buildWrappedScene(
  screens: Observable<out BackStackScreen<*>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup,
  enterAction: ((Scene) -> Unit)? = null
): Scene {
  val myScreens: Observable<out T> = screens.matching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(key.type.jvmName)
  return binding.buildScene(myScreens, viewRegistry, container, enterAction)
}
