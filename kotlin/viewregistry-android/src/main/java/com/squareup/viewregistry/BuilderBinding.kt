/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.viewregistry

import android.content.Context
import android.support.transition.Scene
import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * A [ViewBinding] for [View]s that need to be generated from code.
 * (Use [LayoutBinding] to work with XML layout resources.)
 *
 * It is best for such views to hold off on updating from model objects
 * until they are attached to a window, e.g. to ensure containers have a chance
 * to fire [View.restoreHierarchyState] first. [View.takeWhileAttached] can help
 * with that.
 *
 * Typical usage is to have a custom builder or view's `companion object` implement
 * [ViewBinding] by delegating to one of these:
 *
 *    class MyView(
 *      context: Context,
 *      attributeSet: AttributeSet?
 *    ) : FrameLayout(context, attributeSet) {
 *      // ...
 *      companion object : ViewBuilder<MyScreen>
 *      by BuilderBinding(
 *          type = MyScreen::class.java,
 *          builder = { screens, builders, context, _ ->
 *            MyView(context).apply {
 *              layoutParams = (ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
 *              takeWhileAttached(screens, ::showMyScreen)
 *            }
 *      )
 *    }
 *
 * This pattern allows us to assemble a [ViewRegistry] out of the
 * custom classes themselves.
 *
 *    val TicTacToeViewBuilders = ViewRegistry(
 *        MyView, GamePlayCoordinator, GameOverCoordinator
 *    )
 *
 * Also note that two flavors of [builder] function are supported. Every
 * [builder] must accept an `[Observable]<out [T]>`. Optionally, they can
 * also have a second [ViewRegistry] argument, to allow recursive calls
 * to render nested screens.
 */
class BuilderBinding<T : Any> private constructor(
  override val type: String,
  val builder: (
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup?
  ) -> View
) : ViewBinding<T> {
  constructor(
    type: Class<T>,
    builder: (
      screens: Observable<out T>,
      viewRegistry: ViewRegistry,
      contextForNewView: Context,
      container: ViewGroup?
    ) -> View
  ) : this(type.name, builder)

  constructor(
    type: Class<T>,
    builder: (
      screens: Observable<out T>,
      contextForNewView: Context,
      container: ViewGroup?
    ) -> View
  ) : this(type.name, builder = { screens, _, contextForNewView, container ->
    builder(screens, contextForNewView, container)
  })

  override fun buildView(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup?
  ): View = builder(screens, viewRegistry, contextForNewView, container)

  override fun buildScene(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup,
    enterAction: ((Scene) -> Unit)?
  ): Scene {
    return Scene(
        container, buildView(screens, viewRegistry, contextForNewView, container)
    ).apply { if (enterAction != null) setEnterAction { enterAction(this) } }
  }
}

/**
 * Subscribes [update] to [source] only while this [View] is attached to a window.
 */
fun <S : Any> View.takeWhileAttached(
  source: Observable<S>,
  update: (S) -> Unit
) {
  Coordinators.bind(this) {
    object : Coordinator() {
      var sub: Disposable? = null

      override fun attach(view: View) {
        sub = source.subscribe { screen -> update(screen) }
      }

      override fun detach(view: View) {
        sub!!.dispose()
        sub = null
      }
    }
  }
}
