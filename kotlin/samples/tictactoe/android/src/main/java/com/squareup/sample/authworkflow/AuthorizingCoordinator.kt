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
package com.squareup.sample.authworkflow

import android.view.View
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.LayoutBinding
import com.squareup.workflow.ui.ViewBinding
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

@Suppress("EXPERIMENTAL_API_USAGE")
internal class AuthorizingCoordinator(private val screens: Observable<out AuthorizingScreen>) :
    Coordinator() {
  private val subs = CompositeDisposable()

  override fun attach(view: View) {
    super.attach(view)
    val messageView = view.findViewById<TextView>(R.id.authorizing_message)
    subs.add(screens.map { s -> s.message }
        .subscribe { messageView.text = it })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  companion object : ViewBinding<AuthorizingScreen> by LayoutBinding.of(
      R.layout.authorizing_layout, ::AuthorizingCoordinator
  )
}
