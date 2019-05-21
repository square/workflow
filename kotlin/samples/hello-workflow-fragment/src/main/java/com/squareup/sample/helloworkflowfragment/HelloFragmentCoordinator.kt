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
package com.squareup.sample.helloworkflowfragment

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.helloworkflowfragment.HelloWorkflow.Rendering
import com.squareup.workflow.ui.LayoutBinding
import com.squareup.workflow.ui.ViewBinding
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

@Suppress("EXPERIMENTAL_API_USAGE")
class HelloFragmentCoordinator(
  private val renderings: Observable<out Rendering>
) : Coordinator() {
  private val subs = CompositeDisposable()

  @SuppressLint("SetTextI18n")
  override fun attach(view: View) {
    val messageView = view.findViewById<TextView>(R.id.hello_message)

    subs.add(renderings.subscribe { rendering ->
      messageView.text = rendering.message + " Fragment!"
      messageView.setOnClickListener { rendering.onClick(Unit) }
    })
  }

  override fun detach(view: View) {
    subs.clear()
  }

  companion object : ViewBinding<Rendering> by LayoutBinding.of(
      R.layout.hello_goodbye_layout, ::HelloFragmentCoordinator
  )
}
