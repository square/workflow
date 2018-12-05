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
package com.squareup.viewbuilder

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.coordinators.Coordinators
import com.squareup.viewbuilder.ViewBuilder.Registry
import io.reactivex.Observable

class LayoutViewBuilder<T : Any> private constructor(
  override val type: String,
  @LayoutRes private val layoutId: Int,
  private val coordinatorConstructor: (Observable<out T>, Registry) -> Coordinator
) : ViewBuilder<T> {
  constructor(
    type: Class<T>,
    @LayoutRes layoutId: Int,
    coordinatorConstructor: (Observable<out T>, Registry) -> Coordinator
  ) : this(type.name, layoutId, coordinatorConstructor)

  override fun buildView(
    screens: Observable<out T>,
    builders: Registry,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    return LayoutInflater.from(container?.context ?: contextForNewView)
        .cloneInContext(contextForNewView)
        .inflate(layoutId, container, false)
        .apply {
          Coordinators.bind(this) {
            coordinatorConstructor(screens, builders)
          }
        }
  }

  companion object {
    inline fun <reified T : Any> of(
      @LayoutRes layoutId: Int,
      noinline coordinatorConstructor: (Observable<out T>) -> Coordinator
    ) = of(
        layoutId
    ) { o: Observable<out T>, _ -> coordinatorConstructor(o) }

    inline fun <reified T : Any> of(
      @LayoutRes layoutId: Int,
      noinline coordinatorConstructor: (Observable<out T>, Registry) -> Coordinator
    ): LayoutViewBuilder<T> {
      return LayoutViewBuilder(
          T::class.java, layoutId, coordinatorConstructor
      )
    }
  }
}
