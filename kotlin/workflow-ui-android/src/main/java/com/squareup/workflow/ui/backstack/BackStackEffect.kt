package com.squareup.workflow.ui.backstack

import android.support.transition.Scene
import android.view.View
import android.view.ViewGroup
import com.squareup.workflow.ui.BackStackScreen
import com.squareup.workflow.ui.BackStackScreen.Key
import com.squareup.workflow.ui.ExperimentalWorkflowUi
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.backstack.ViewStateStack.Direction
import com.squareup.workflow.ui.buildScene
import com.squareup.workflow.ui.buildView
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
@ExperimentalWorkflowUi
interface BackStackEffect {
  /**
   * Returns true of this effect can be applied to the given keys and direction.
   * The default implementation always returns true, indicating that this effect
   * can handle all transitions.
   */
  fun matches(
    from: Key<*>,
    to: Key<*>,
    direction: ViewStateStack.Direction
  ): Boolean = true

  /**
   * Perform the transition. Remember that:
   *
   *  - You can use [View.backStackKey] to extract the key associated with [from].
   *
   *  - You can use [viewForWrappedScreen] or [sceneForWrappedScreen] to give the new view
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
 * received via [backstackScreens].
 */
@ExperimentalWorkflowUi
fun <T : Any> BackStackScreen<T>.viewForWrappedScreen(
  backstackScreens: Observable<out BackStackScreen<*>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup
): View {
  val wrappedScreens: Observable<out T> = backstackScreens.mapToWrappedMatching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(key.type.jvmName)
  return binding.buildView(wrappedScreens, viewRegistry, container)
}

/**
 * Fishes in [viewRegistry] for the [ViewBinding] for type [T] and
 * uses it to instantiate a [Scene] to display any matching items
 * received via [backstackScreens].
 */
@ExperimentalWorkflowUi
fun <T : Any> BackStackScreen<T>.sceneForWrappedScreen(
  backstackScreens: Observable<out BackStackScreen<*>>,
  viewRegistry: ViewRegistry,
  container: ViewGroup,
  enterAction: ((Scene) -> Unit)? = null
): Scene {
  val wrappedScreens: Observable<out T> = backstackScreens.mapToWrappedMatching(this)
  val binding: ViewBinding<T> = viewRegistry.getBinding(key.type.jvmName)
  return binding.buildScene(wrappedScreens, viewRegistry, container, enterAction)
}
