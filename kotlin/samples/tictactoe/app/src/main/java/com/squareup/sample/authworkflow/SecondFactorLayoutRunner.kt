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
import androidx.appcompat.widget.Toolbar
import com.squareup.sample.tictactoe.R
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.backPressedHandler

internal class SecondFactorLayoutRunner(
  private val view: View
) : LayoutRunner<SecondFactorScreen> {
  private val toolbar: Toolbar = view.findViewById(R.id.second_factor_toolbar)
  private val error: TextView = view.findViewById(R.id.second_factor_error_message)
  private val secondFactor: EditText = view.findViewById(R.id.second_factor)
  private val button: Button = view.findViewById(R.id.second_factor_submit_button)

  override fun showRendering(
    rendering: SecondFactorScreen,
    containerHints: ContainerHints
  ) {
    view.backPressedHandler = { rendering.onCancel() }
    toolbar.setNavigationOnClickListener { rendering.onCancel() }

    error.text = rendering.errorMessage

    button.setOnClickListener {
      rendering.onSubmit(secondFactor.text.toString())
    }
  }

  companion object : ViewBinding<SecondFactorScreen> by bind(
      R.layout.second_factor_layout, ::SecondFactorLayoutRunner
  )
}
