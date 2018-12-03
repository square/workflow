/*
 * Copyright 2017 Square Inc.
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
package com.squareup.sample.authgameapp

import android.view.View
import android.view.ViewGroup
import com.squareup.coordinators.Coordinator
import com.squareup.viewbuilder.HandlesBack.Helper.onBackPressed
import com.squareup.viewbuilder.setBackHandler
import com.squareup.workflow.AnyScreen
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class LogoutCoordinator(
  private val screens: Observable<out AnyScreen>,
  private val onLogout: () -> Unit
) : Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var logoutButton: View
  private lateinit var frame: ViewGroup

  override fun attach(view: View) {
    super.attach(view)
    logoutButton = view.findViewById(R.id.logout_button)
    frame = view.findViewById(R.id.logout_decorator_body)

    subs.add(screens
        .subscribe {
          // Give the wrapped view a chance to handle back button events. If it declines,
          // log out.
          view.setBackHandler {
            if (!onBackPressed(frame.getChildAt(0))) onLogout()
          }
          logoutButton.setOnClickListener { onLogout() }
        })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }
}
