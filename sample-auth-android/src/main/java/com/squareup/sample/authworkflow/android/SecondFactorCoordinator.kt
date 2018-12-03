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
package com.squareup.sample.authworkflow.android

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.authworkflow.SecondFactorScreen
import com.squareup.sample.authworkflow.SubmitSecondFactor
import com.squareup.viewbuilder.LayoutViewBuilder
import com.squareup.viewbuilder.ViewBuilder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

internal class SecondFactorCoordinator(private val screens: Observable<out SecondFactorScreen>) :
    Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var error: TextView
  private lateinit var secondFactor: EditText
  private lateinit var button: Button

  override fun attach(view: View) {
    super.attach(view)

    error = view.findViewById(R.id.second_factor_error_message)
    secondFactor = view.findViewById(R.id.second_factor)
    button = view.findViewById(R.id.second_factor_submit_button)

    subs.add(screens.subscribe { this.update(it) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(screen: SecondFactorScreen) {
    error.text = screen.data

    button.setOnClickListener {
      screen.onEvent(SubmitSecondFactor(secondFactor.text.toString()))
    }
  }

  companion object : ViewBuilder<SecondFactorScreen> by LayoutViewBuilder.of(
      R.layout.second_factor_layout, ::SecondFactorCoordinator
  )
}
