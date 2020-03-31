/*
 * Copyright 2020 Square Inc.
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
package com.squareup.sample.porchbox.view

import android.view.View
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.squareup.sample.porchbox.LoginRendering
import com.squareup.sample.porchbox.R
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory

/**
 * Renders the screen for entering login credentials.
 */
class LoginLayoutRunner(private val view: View) : LayoutRunner<LoginRendering> {

  private val emailInput = view.findViewById<TextInputEditText>(R.id.input_email)
  private val passInput = view.findViewById<TextInputEditText>(R.id.input_password)
  private val button = view.findViewById<Button>(R.id.login_button)

  override fun showRendering(
    rendering: LoginRendering,
    viewEnvironment: ViewEnvironment
  ) {
    button.setOnClickListener {
      rendering.onLogin(emailInput.text?.toString() ?: "empty", passInput.text?.toString() ?: "")
    }
  }

  companion object : ViewFactory<LoginRendering> by LayoutRunner.bind(
      R.layout.login_layout,
      ::LoginLayoutRunner
  )

}