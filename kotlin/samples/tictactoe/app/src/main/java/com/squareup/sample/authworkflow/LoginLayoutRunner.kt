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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.backPressedHandler

internal class LoginLayoutRunner(val view: View) : LayoutRunner<LoginScreen> {
  private val error: TextView = view.findViewById(R.id.login_error_message)
  private val email: EditText = view.findViewById(R.id.login_email)
  private val password: EditText = view.findViewById(R.id.login_password)
  private val loginButton: Button = view.findViewById(R.id.login_button)

  override fun showRendering(rendering: LoginScreen) {
    error.text = rendering.errorMessage

    loginButton.setOnClickListener {
      rendering.onLogin(email.text.toString(), password.text.toString())
    }

    view.backPressedHandler = { rendering.onCancel() }
  }

  companion object : ViewBinding<LoginScreen> by bind(
      R.layout.login_layout, ::LoginLayoutRunner
  )
}
