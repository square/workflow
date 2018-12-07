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

import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.viewbuilder.ViewBuilder.Registry
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class ViewStackCoordinator(
  private val screens: Observable<out StackScreen<*>>,
  private val builders: ViewBuilder.Registry
) : Coordinator() {
  private val subs = CompositeDisposable()

  override fun attach(view: View) {
    val viewStackLayout = view.findViewById<ViewStackFrameLayout>(
        R.id.view_stack
    )

    subs.add(screens.distinctUntilChanged { a, b -> a.key == b.key }
        .map { stackScreen -> stackScreen.buildWrappedView(screens, builders, viewStackLayout) }
        .subscribe { viewStackLayout.show(it) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  companion object : ViewBuilder<StackScreen<*>> by LayoutViewBuilder(
      type = StackScreen::class.java,
      layoutId = R.layout.view_stack_layout,
      coordinatorConstructor = ::ViewStackCoordinator
  )
}

fun <T : Any> StackScreen<T>.buildWrappedView(
  screens: Observable<out StackScreen<*>>,
  builders: Registry,
  container: ViewGroup
): View {
  val myScreens: Observable<out T> = screens.matchingWrappedScreens(this)
  val builder: ViewBuilder<T> = builders[key.type.name]
  return builder.buildView(myScreens, builders, container)
      .apply { betterKey = this@buildWrappedView.key }
}

var View.betterKey: ViewStackKey<*>
  get() {
    return getTag(R.id.view_stack_key) as ViewStackKey<*>?
        ?: throw IllegalArgumentException("No key found on $this")
  }
  set(screenKey) = setTag(R.id.view_stack_key, screenKey)
