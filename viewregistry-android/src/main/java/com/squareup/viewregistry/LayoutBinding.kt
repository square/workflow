/*
 * Copyright 2018 Square Inc.
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
import android.support.annotation.LayoutRes
import android.transition.Scene
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import io.reactivex.Observable

/**
 * A [ViewBinding] built from a [layoutId] and a [coordinatorConstructor].
 * (Use [BuilderBinding] to create views from code.)
 *
 * Typical usage is to have a [Coordinator]'s `companion object` implement
 * [ViewBinding] by delegating to one of these, tied to the layout resource
 * it typically expects to drive.
 *
 *    class NewGameCoordinator(
 *       private val screens: Observable<out NewGameScreen>
 *    ) : Coordinator() {
 *      // ...
 *      companion object : ViewBuilder<NewGameScreen> by LayoutViewBuilder.of(
 *          R.layout.new_game_layout, ::NewGameCoordinator
 *      )
 *    }
 *
 * This pattern allows us to assemble a [ViewRegistry] out of the
 * [Coordinator] classes themselves.
 *
 *    val TicTacToeViewBuilders = ViewRegistry(
 *        NewGameCoordinator, GamePlayCoordinator, GameOverCoordinator
 *    )
 *
 * Also note that two flavors of [coordinatorConstructor] are supported. Every
 * [Coordinator] must accept an `[Observable]<out [T]>`. Optionally, they can
 * also have a second [ViewRegistry] argument, to allow recursive calls
 * to render nested screens.
 */
class LayoutBinding<T : Any> private constructor(
  override val type: String,
  @LayoutRes private val layoutId: Int,
  private val coordinatorConstructor: (Observable<out T>, ViewRegistry) -> Coordinator
) : ViewBinding<T> {
  constructor(
    type: Class<T>,
    @LayoutRes layoutId: Int,
    coordinatorConstructor: (Observable<out T>, ViewRegistry) -> Coordinator
  ) : this(type.name, layoutId, coordinatorConstructor)

  override fun buildView(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    return LayoutInflater.from(container?.context ?: contextForNewView)
        .cloneInContext(contextForNewView)
        .inflate(layoutId, container, false)
        .apply {
          Coordinators.bind(this) {
            coordinatorConstructor(screens, viewRegistry)
          }
        }
  }

  override fun buildScene(
    screens: Observable<out T>,
    viewRegistry: ViewRegistry,
    contextForNewView: Context,
    container: ViewGroup,
    enterAction: ((Scene) -> Unit)?
  ): Scene {
    return Scene.getSceneForLayout(container, layoutId, contextForNewView)
        .apply {
          setEnterAction {
            if (enterAction != null) enterAction(this)
            viewOrNull()?.let {
              Coordinators.bind(it) { coordinatorConstructor(screens, viewRegistry) }
            }
          }
        }
  }

  companion object {
    inline fun <reified T : Any> of(
      @LayoutRes layoutId: Int,
      noinline coordinatorConstructor: (Observable<out T>) -> Coordinator
    ) = of(layoutId) { screens: Observable<out T>, _ -> coordinatorConstructor(screens) }

    inline fun <reified T : Any> of(
      @LayoutRes layoutId: Int,
      noinline coordinatorConstructor: (Observable<out T>, ViewRegistry) -> Coordinator
    ): LayoutBinding<T> {
      return LayoutBinding(
          T::class.java, layoutId, coordinatorConstructor
      )
    }
  }
}
