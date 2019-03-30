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

import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.squareup.coordinators.Coordinator
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.CancelSecondFactor
import com.squareup.sample.authworkflow.SecondFactorScreen.Event.SubmitSecondFactor
import com.squareup.workflow.ui.LayoutBinding
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.setBackHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import com.squareup.sample.tictactoe.R

internal class SecondFactorCoordinator(private val screens: Observable<out SecondFactorScreen>) :
    Coordinator() {
  private val subs = CompositeDisposable()

  private lateinit var toolbar: Toolbar
  private lateinit var error: TextView
  private lateinit var secondFactor: EditText
  private lateinit var button: Button

  override fun attach(view: View) {
    super.attach(view)

    toolbar = view.findViewById(R.id.second_factor_toolbar)
    error = view.findViewById(R.id.second_factor_error_message)
    secondFactor = view.findViewById(R.id.second_factor)
    button = view.findViewById(R.id.second_factor_submit_button)

    subs.add(screens.subscribe { this.update(it, view) })
  }

  override fun detach(view: View) {
    subs.clear()
    super.detach(view)
  }

  private fun update(
    screen: SecondFactorScreen,
    view: View
  ) {
    view.setBackHandler { screen.onEvent(CancelSecondFactor) }
    toolbar.setNavigationOnClickListener { screen.onEvent(CancelSecondFactor) }

    error.text = screen.errorMessage

    button.setOnClickListener {
      screen.onEvent(SubmitSecondFactor(secondFactor.text.toString()))
    }
  }

  companion object : ViewBinding<SecondFactorScreen> by LayoutBinding.of(
      R.layout.second_factor_layout, ::SecondFactorCoordinator
  )
}
